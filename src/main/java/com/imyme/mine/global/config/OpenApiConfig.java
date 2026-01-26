package com.imyme.mine.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 설정
 * - JWT 인증 스키마 추가
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "JWT";

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        Components components =
                new Components()
                        .addSecuritySchemes(
                                jwtSchemeName,
                                new SecurityScheme()
                                        .name(jwtSchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"));

        Server prodServer = new Server();
        prodServer.setUrl("https://imymemine.kr/server");
        prodServer.setDescription("운영 서버 (Production)");

        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("로컬 서버 (Local)");

        return new OpenAPI()
            .info(apiInfo())
            .addSecurityItem(securityRequirement)
            .components(components)
            .servers(List.of(prodServer, localServer));
    }

    private Info apiInfo() {
        return new Info()
                .title("Mine API Documentation")
                .description("Mine 프로젝트 API 문서")
                .version("v1.0.0");
    }
}
