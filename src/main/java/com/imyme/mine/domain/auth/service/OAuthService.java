package com.imyme.mine.domain.auth.service;

import com.imyme.mine.domain.auth.client.KakaoOAuthClient;
import com.imyme.mine.domain.auth.dto.OAuthLoginRequest;
import com.imyme.mine.domain.auth.dto.OAuthLoginResponse;
import com.imyme.mine.domain.auth.entity.User;
import com.imyme.mine.domain.auth.repository.UserRepository;
import com.imyme.mine.global.secret.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 카카오 로그인 메인 로직
     */
    @Transactional
    public OAuthLoginResponse loginWithKakao(OAuthLoginRequest request) {

        // 1. 카카오 API 호출 (토큰 발급 + 유저 정보 조회)
        // 트랜잭션 범위 밖에서 외부 통신을 수행하는 것이 성능상 좋음
        KakaoOAuthClient.KakaoUserInfo userInfo = fetchKakaoUserInfo(request);

        // 2. 우리 서비스 전용 OAuth ID 생성 (예: kakao_12345)
        String oauthId = "kakao_" + userInfo.getId();

        // 3. 로그인 또는 회원가입 처리
        return processLogin(oauthId, userInfo);
    }

    private KakaoOAuthClient.KakaoUserInfo fetchKakaoUserInfo(OAuthLoginRequest request) {
        KakaoOAuthClient.KakaoTokenResponse tokenResponse =
            kakaoOAuthClient.getAccessToken(request.getCode(), request.getRedirectUri());

        return kakaoOAuthClient.getUserInfo(tokenResponse.getAccessToken());
    }

    @Transactional
    protected OAuthLoginResponse processLogin(String oauthId, KakaoOAuthClient.KakaoUserInfo userInfo) {

        Optional<User> existingUser = userRepository.findByOauthId(oauthId);
        User user;
        boolean isNewUser;

        if (existingUser.isPresent()) {
            // [기존 회원]
            user = existingUser.get();
            isNewUser = false;

            // 마지막 접속일 업데이트
            user.setLastLoginAt(LocalDateTime.now());
            // (연속 접속일 로직 등은 나중에 추가)

            log.info("기존 유저 로그인: {}", user.getId());
        } else {
            // [신규 회원]
            user = createNewUser(oauthId, userInfo);
            isNewUser = true;
            log.info("신규 유저 가입: {}", user.getId());
        }

        // 4. Access Token 발급 (MVP 단계라 Refresh Token은 생략)
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());

        // 5. 응답 생성
        return OAuthLoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(null) // MVP에서는 null 처리
            .deviceId(null)     // MVP에서는 null 처리
            .user(OAuthLoginResponse.UserInfo.builder()
                .id(user.getId())
                .oauthId(user.getOauthId())
                .oauthProvider(user.getOauthProvider())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .level(user.getLevel())
                .isNewUser(isNewUser)
                .build())
            .build();
    }

    private User createNewUser(String oauthId, KakaoOAuthClient.KakaoUserInfo userInfo) {
        String baseNickname = "사용자";
        String profileImage = null;
        String email = null;

        try {
            if (userInfo.getKakaoAccount() != null) {
                email = userInfo.getKakaoAccount().getEmail();
                if (userInfo.getKakaoAccount().getProfile() != null) {
                    baseNickname = userInfo.getKakaoAccount().getProfile().getNickname();
                    profileImage = userInfo.getKakaoAccount().getProfile().getProfileImageUrl();
                }
            }
        } catch (Exception e) {
            log.warn("카카오 정보 파싱 실패 (기본값 사용): {}", e.getMessage());
        }

        // 닉네임 중복 방지 로직 적용
        String uniqueNickname = generateUniqueNickname(baseNickname);

        User user = User.builder()
            .oauthId(oauthId)
            .oauthProvider("KAKAO")
            .email(email)
            .nickname(uniqueNickname)
            .profileImageUrl(profileImage)
            .role("USER")
            .level(1)
            .totalCardCount(0)
            .activeCardCount(0)
            .consecutiveDays(1)
            .winCount(0)
            .build();

        return userRepository.save(user);
    }

    /**
     * 닉네임 중복 방지 (기본닉네임 + 랜덤숫자)
     */
    private String generateUniqueNickname(String baseNickname) {
        SecureRandom random = new SecureRandom();
        String nickname = baseNickname;

        // 1. 일단 닉네임만으로 존재 여부 확인
        if (!userRepository.existsByNickname(nickname)) {
            return nickname;
        }

        // 2. 중복되면 뒤에 랜덤 숫자 붙여서 시도
        while (true) {
            int suffix = random.nextInt(9999);
            String candidate = baseNickname + "#" + suffix;
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
    }
}
