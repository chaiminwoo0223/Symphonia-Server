package com.symphonia.auth.presentation.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.UnitTest;
import com.symphonia.global.config.properties.RefreshTokenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

@DisplayName("CookieProvider 단위 테스트")
class CookieProviderTest extends UnitTest {

    private static final long EXPIRATION_TIME = 604800L;
    private static final String REFRESH_TOKEN = "refresh-token-value";

    private CookieProvider cookieProvider;

    @BeforeEach
    void setUp() {
        cookieProvider = new CookieProvider(new RefreshTokenProperties(EXPIRATION_TIME, true));
    }

    @Nested
    @DisplayName("create 메서드는")
    class Create {

        @Test
        @DisplayName("HttpOnly, Secure, SameSite=Strict 속성을 가진 쿠키를 만든다.")
        void shouldCreateCookieWithSecurityAttributes() {
            // when
            ResponseCookie cookie = cookieProvider.create(REFRESH_TOKEN);

            // then
            assertThat(cookie.getValue()).isEqualTo(REFRESH_TOKEN);
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecure()).isTrue();
            assertThat(cookie.getSameSite()).isEqualTo("Strict");
            assertThat(cookie.getPath()).isEqualTo("/api/v1/auth/refresh");
            assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(EXPIRATION_TIME);
        }
    }

    @Nested
    @DisplayName("expire 메서드는")
    class Expire {

        @Test
        @DisplayName("Max-Age가 0인 쿠키를 만든다.")
        void shouldCreateCookieWithZeroMaxAge() {
            // when
            ResponseCookie cookie = cookieProvider.expire();

            // then
            assertThat(cookie.getMaxAge().getSeconds()).isZero();
        }
    }
}
