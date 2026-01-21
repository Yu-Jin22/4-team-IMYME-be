package com.imyme.mine.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 마스터 데이터 API는 공개
                .requestMatchers("/categories/**", "/keywords/**").permitAll()
                // 그 외는 일단 인증 필요 (나중에 카카오 로그인 붙이면 조정)
                .anyRequest().authenticated()
            )
            .build();
    }
}
