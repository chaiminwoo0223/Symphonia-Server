package com.symphonia.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;

@Profile("!prod")
@Configuration
public class SwaggerConfig {
    private static final String SERVER_NAME = "Symphonia";
    private static final String GITHUB_URL = "https://github.com/chaiminwoo0223/Symphonia-Server";
    private static final String API_TITLE = "Symphonia 서버 API 문서";
    private static final String API_DESCRIPTION = "Symphonia 서버 API 문서입니다.";
    private static final String BEARER_AUTH = "bearerAuth";
    private static final String BEARER = "bearer";
    private static final String JWT = "JWT";
    private static final String VERSION = "v1";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(securityRequirement()) // 전역 보안 요구사항 적용 (모든 API에 JWT 인증 적용)
                .components(securityComponents()) // JWT 보안 스킴 컴포넌트 등록
                .info(apiInfo()); // API 문서 기본 정보 설정
    }

    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList(BEARER_AUTH);
    }

    private Components securityComponents() {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme(BEARER)
                .bearerFormat(JWT)
                .in(SecurityScheme.In.HEADER)
                .name(HttpHeaders.AUTHORIZATION);

        return new Components().addSecuritySchemes(BEARER_AUTH, securityScheme);
    }

    private Info apiInfo() {
        License license = new License()
                .name(SERVER_NAME)
                .url(GITHUB_URL);

        return new Info()
                .title(API_TITLE)
                .description(API_DESCRIPTION)
                .version(VERSION)
                .license(license);
    }
}
