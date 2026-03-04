package com.imyme.mine.domain.notification.entity;

/**
 * 알림 타입
 */
public enum NotificationType {
    LEVEL_UP,          // 레벨업
    CARD_COMPLETE,     // 카드 학습 완료
    PVP_MATCHED,       // PvP 매칭 완료
    PVP_RESULT,        // PvP 대결 결과
    CHALLENGE_OPEN,    // 챌린지 오픈
    CHALLENGE_RESULT,  // 챌린지 결과
    SYSTEM             // 시스템 공지
}