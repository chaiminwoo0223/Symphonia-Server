package com.symphonia.global.config;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// 지표 수집과 헬스 확인은 토큰 없이 부른다. 노출하는 엔드포인트는 설정이 정하고, 이 포트의 외부 차단은 네트워크가 맡는다.
// management 포트가 앱 포트와 다르면 EndpointRequest는 management 포트의 요청에만 맞으므로, 앱 포트에는 아무것도 열리지 않는다.
@Configuration
public class ActuatorSecurityConfig {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) {
        http.securityMatcher(EndpointRequest.toAnyEndpoint())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
