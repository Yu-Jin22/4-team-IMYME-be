package com.imyme.mine.domain.card.service;

import com.imyme.mine.domain.ai.client.AiServerClient;
import com.imyme.mine.domain.card.dto.AttemptCreateRequest;
import com.imyme.mine.domain.card.dto.AttemptCreateResponse;
import com.imyme.mine.domain.card.dto.AttemptDetailResponse;
import com.imyme.mine.domain.card.dto.UploadCompleteRequest;
import com.imyme.mine.domain.card.dto.UploadCompleteResponse;
import com.imyme.mine.domain.card.entity.AttemptStatus;
import com.imyme.mine.domain.card.entity.Card;
import com.imyme.mine.domain.card.entity.CardAttempt;
import com.imyme.mine.domain.card.entity.CardFeedback;
import com.imyme.mine.domain.card.repository.CardAttemptRepository;
import com.imyme.mine.domain.card.repository.CardFeedbackRepository;
import com.imyme.mine.domain.card.repository.CardRepository;
import com.imyme.mine.global.config.S3Properties;
import com.imyme.mine.global.error.BusinessException;
import com.imyme.mine.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttemptService {

    private final CardRepository cardRepository;
    private final CardAttemptRepository cardAttemptRepository;
    private final CardFeedbackRepository cardFeedbackRepository;
    private final AiServerClient aiServerClient;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    private static final int MAX_ATTEMPTS_PER_CARD = 5;
    private static final Duration UPLOAD_EXPIRATION = Duration.ofMinutes(10);
    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofHours(1);

    @Transactional
    public AttemptCreateResponse createAttempt(Long userId, Long cardId, AttemptCreateRequest request) {
        log.debug("시도 생성 시작 - userId: {}, cardId: {}", userId, cardId);

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        long attemptCount = cardAttemptRepository.countByCardId(card.getId());
        if (attemptCount >= MAX_ATTEMPTS_PER_CARD) {
            throw new BusinessException(ErrorCode.MAX_ATTEMPTS_EXCEEDED);
        }

        Short nextAttemptNo = calculateNextAttemptNo(card.getId());

        CardAttempt attempt = CardAttempt.builder()
            .card(card)
            .attemptNo(nextAttemptNo)
            .status(AttemptStatus.PENDING)
            .durationSeconds(request.durationSeconds())
            .build();

        CardAttempt savedAttempt = cardAttemptRepository.save(attempt);

        LocalDateTime expiresAt = LocalDateTime.now().plus(UPLOAD_EXPIRATION);

        log.info("시도 생성 완료 - attemptId: {}, attemptNo: {}", savedAttempt.getId(), savedAttempt.getAttemptNo());

        return AttemptCreateResponse.of(savedAttempt, expiresAt);
    }

    @Transactional(readOnly = true)
    public AttemptDetailResponse getAttemptDetail(Long userId, Long cardId, Long attemptId) {
        log.debug("시도 상세 조회 - userId: {}, cardId: {}, attemptId: {}", userId, cardId, attemptId);

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        CardAttempt attempt = cardAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ATTEMPT_NOT_FOUND));

        if (!attempt.getCard().getId().equals(card.getId())) {
            throw new BusinessException(ErrorCode.INVALID_CARD_ATTEMPT_MISMATCH);
        }

        return buildAttemptDetailResponse(attempt);
    }

    @Transactional
    public UploadCompleteResponse uploadComplete(Long userId, Long cardId, Long attemptId, UploadCompleteRequest request) {
        log.debug("업로드 완료 처리 시작 - userId: {}, cardId: {}, attemptId: {}", userId, cardId, attemptId);

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        CardAttempt attempt = cardAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ATTEMPT_NOT_FOUND));

        if (!attempt.getCard().getId().equals(card.getId())) {
            throw new BusinessException(ErrorCode.INVALID_CARD_ATTEMPT_MISMATCH);
        }

        if (attempt.getStatus() != AttemptStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }

        LocalDateTime expiresAt = attempt.getCreatedAt().plus(UPLOAD_EXPIRATION);
        if (LocalDateTime.now().isAfter(expiresAt)) {
            throw new BusinessException(ErrorCode.UPLOAD_EXPIRED);
        }

        attempt.markUploaded(request.audioUrl(), request.durationSeconds());

        // STT (Speech-to-Text) 처리 - 동기 호출
        try {
            log.debug("STT 처리 시작 - attemptId: {}, audioUrl: {}", attemptId, request.audioUrl());

            // S3 객체 URL에서 objectKey 추출
            String objectKey = extractObjectKeyFromUrl(request.audioUrl());

            // 읽기용 Presigned URL 생성 (AI 서버가 S3에서 다운로드 가능)
            String readPresignedUrl = generateReadPresignedUrl(objectKey);
            log.debug("읽기용 Presigned URL 생성 완료 - objectKey: {}", objectKey);

            // AI 서버에 읽기용 URL 전달
            String sttText = aiServerClient.transcribe(readPresignedUrl);
            attempt.complete(sttText);
            log.info("STT 처리 성공 - attemptId: {}, status: COMPLETED, 텍스트 길이: {}", attemptId, sttText.length());
        } catch (BusinessException e) {
            // AI 서버 오류 시 FAILED 상태로 변경
            String errorMessage = e.getMessage();
            attempt.fail(errorMessage);
            log.error("STT 처리 실패 - attemptId: {}, status: FAILED, error: {}", attemptId, errorMessage);
        }

        log.info("업로드 완료 처리 완료 - attemptId: {}, status: {}", attemptId, attempt.getStatus());

        return UploadCompleteResponse.from(attempt);
    }

    @Transactional
    public void deleteAttempt(Long userId, Long cardId, Long attemptId) {
        log.debug("학습 시도 삭제 시작 - userId: {}, cardId: {}, attemptId: {}", userId, cardId, attemptId);

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        CardAttempt attempt = cardAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ATTEMPT_NOT_FOUND));

        if (!attempt.getCard().getId().equals(card.getId())) {
            throw new BusinessException(ErrorCode.INVALID_CARD_ATTEMPT_MISMATCH);
        }

        if (attempt.isDeleted()) {
            throw new BusinessException(ErrorCode.ALREADY_DELETED);
        }

        // 삭제 가능한 상태: PENDING, FAILED, EXPIRED만 허용
        // 삭제 불가 상태: UPLOADED(AI 대기 중), PROCESSING(처리 중), COMPLETED(결과 보존 필요)
        if (attempt.getStatus() == AttemptStatus.UPLOADED ||
            attempt.getStatus() == AttemptStatus.PROCESSING ||
            attempt.getStatus() == AttemptStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_UPLOADED);
        }

        attempt.softDelete();

        log.info("학습 시도 삭제 완료 - attemptId: {}, status: {}", attemptId, attempt.getStatus());
    }

    private Short calculateNextAttemptNo(Long cardId) {
        Short maxAttemptNo = cardAttemptRepository.findMaxAttemptNoByCardId(cardId);
        return (maxAttemptNo == null) ? 1 : (short) (maxAttemptNo + 1);
    }

    private AttemptDetailResponse buildAttemptDetailResponse(CardAttempt attempt) {
        return switch (attempt.getStatus()) {
            case PENDING -> {
                LocalDateTime expiresAt = attempt.getCreatedAt().plus(UPLOAD_EXPIRATION);
                yield AttemptDetailResponse.fromPending(attempt, expiresAt);
            }
            case UPLOADED -> AttemptDetailResponse.fromUploaded(attempt);
            case PROCESSING -> AttemptDetailResponse.fromProcessing(attempt);
            case COMPLETED -> {
                // CardFeedback 조회 (1:1 관계)
                CardFeedback feedback = cardFeedbackRepository.findByAttemptId(attempt.getId())
                    .orElse(null);
                yield AttemptDetailResponse.fromCompleted(attempt, feedback);
            }
            case FAILED -> AttemptDetailResponse.fromFailed(attempt);
            case EXPIRED -> AttemptDetailResponse.fromExpired(attempt);
        };
    }

    /**
     * S3 URL에서 objectKey 추출
     * 예: https://bucket.s3.region.amazonaws.com/audios/6/17/file.wav -> audios/6/17/file.wav
     */
    private String extractObjectKeyFromUrl(String s3Url) {
        try {
            // S3 URL 형식: https://{bucket}.s3.{region}.amazonaws.com/{objectKey}
            String bucketPrefix = s3Properties.getBucket() + ".s3.";
            int keyStartIndex = s3Url.indexOf(bucketPrefix);
            if (keyStartIndex == -1) {
                throw new BusinessException(ErrorCode.INVALID_AUDIO_URL);
            }

            // .amazonaws.com/ 이후부터가 objectKey
            int objectKeyStart = s3Url.indexOf(".amazonaws.com/");
            if (objectKeyStart == -1) {
                throw new BusinessException(ErrorCode.INVALID_AUDIO_URL);
            }

            return s3Url.substring(objectKeyStart + ".amazonaws.com/".length());
        } catch (Exception e) {
            log.error("S3 URL에서 objectKey 추출 실패 - url: {}", s3Url, e);
            throw new BusinessException(ErrorCode.INVALID_AUDIO_URL);
        }
    }

    /**
     * 읽기용 Presigned URL 생성 (AI 서버가 S3에서 파일 다운로드용)
     */
    private String generateReadPresignedUrl(String objectKey) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRATION)
                .getObjectRequest(builder -> builder
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                )
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("읽기용 Presigned URL 생성 실패 - objectKey: {}", objectKey, e);
            throw new BusinessException(ErrorCode.S3_UPLOAD_ERROR);
        }
    }
}