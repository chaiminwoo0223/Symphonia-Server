package com.symphonia.gathering.infrastructure.sender.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 메시지에 담는 개인 링크 웹 페이지의 주소다. 토큰은 이 주소 뒤에 fragment(#토큰)로 붙인다.
// fragment는 브라우저가 서버로 보내지 않아, 웹 서버 접근 로그와 Referer에 토큰이 남지 않는다.
@ConfigurationProperties(prefix = "gathering.notification.link")
public record NotificationLinkProperties(String baseUrl) {
    public String urlOf(String linkToken) {
        return baseUrl + "#" + linkToken;
    }
}
