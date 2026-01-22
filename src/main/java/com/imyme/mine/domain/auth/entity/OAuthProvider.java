package com.imyme.mine.domain.auth.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OAuth 제공자 타입
 */
@Getter
@RequiredArgsConstructor
public enum OAuthProvider {
    KAKAO("kakao", "카카오"),
    GOOGLE("google", "구글"),
    APPLE("apple", "애플");

    private final String code;
    private final String displayName;

    /**
     * 문자열로부터 OAuthProvider 찾기
     *
     * @param code provider 코드 (kakao, google, apple)
     * @return OAuthProvider
     * @throws IllegalArgumentException 유효하지 않은 provider인 경우
     */
    public static OAuthProvider fromCode(String code) {
        for (OAuthProvider provider : values()) {
            if (provider.getCode().equalsIgnoreCase(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Invalid OAuth provider: " + code);
    }

    /**
     * 문자열이 유효한 OAuth provider인지 확인
     *
     * @param code provider 코드
     * @return 유효 여부
     */
    public static boolean isValid(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}