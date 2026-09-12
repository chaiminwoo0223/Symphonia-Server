package com.symphonia.global.security.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.symphonia.UnitTest;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.global.security.writer.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@DisplayName("RateLimitFilter 단위 테스트")
class RateLimitFilterTest extends UnitTest {

    private static final String IP = "127.0.0.1";

    @InjectMocks private RateLimitFilter rateLimitFilter;

    @Mock private StringRedisTemplate redisTemplate;

    @Mock private ErrorResponseWriter errorResponseWriter;

    @Mock private ValueOperations<String, String> valueOperations;

    @Mock private HttpServletRequest request;

    @Mock private HttpServletResponse response;

    @Mock private FilterChain filterChain;

    @Nested
    @DisplayName("doFilterInternal 메서드는")
    class DoFilterInternal {

        @Nested
        @DisplayName("요청 경로가 rate limit 대상이 아닌 경우")
        class WhenPathNotLimited {

            @BeforeEach
            void setUp() {
                given(request.getRequestURI()).willReturn("/api/v1/members/me");
            }

            @Test
            @DisplayName("Redis를 조회하지 않고 다음 필터로 그대로 통과시킨다")
            void shouldPassThroughWithoutCheckingRedis() throws Exception {
                // when
                rateLimitFilter.doFilterInternal(request, response, filterChain);

                // then
                verify(filterChain).doFilter(request, response);
                verify(redisTemplate, never()).opsForValue();
            }
        }

        @Nested
        @DisplayName("요청 경로가 rate limit 대상인 경우")
        class WhenPathLimited {

            @BeforeEach
            void setUp() {
                given(request.getRequestURI()).willReturn("/api/v1/auth/login");
                given(request.getRemoteAddr()).willReturn(IP);
                given(redisTemplate.opsForValue()).willReturn(valueOperations);
            }

            @Nested
            @DisplayName("해당 윈도우의 첫 요청인 경우")
            class WhenFirstRequestInWindow {

                @BeforeEach
                void setUp() {
                    given(valueOperations.increment("rate-limit:/api/v1/auth/login:" + IP))
                            .willReturn(1L);
                }

                @Test
                @DisplayName("카운터에 만료 시간을 설정하고 다음 필터로 통과시킨다")
                void shouldSetExpirationAndPassThrough() throws Exception {
                    // when
                    rateLimitFilter.doFilterInternal(request, response, filterChain);

                    // then
                    verify(redisTemplate)
                            .expire("rate-limit:/api/v1/auth/login:" + IP, Duration.ofMinutes(1));
                    verify(filterChain).doFilter(request, response);
                }
            }

            @Nested
            @DisplayName("요청 횟수가 임계값 이내인 경우")
            class WhenWithinLimit {

                @BeforeEach
                void setUp() {
                    given(valueOperations.increment("rate-limit:/api/v1/auth/login:" + IP))
                            .willReturn(5L);
                }

                @Test
                @DisplayName("다음 필터로 통과시킨다")
                void shouldPassThrough() throws Exception {
                    // when
                    rateLimitFilter.doFilterInternal(request, response, filterChain);

                    // then
                    verify(filterChain).doFilter(request, response);
                    verify(errorResponseWriter, never()).send(any(), any());
                }
            }

            @Nested
            @DisplayName("요청 횟수가 임계값을 초과한 경우")
            class WhenLimitExceeded {

                @BeforeEach
                void setUp() {
                    given(valueOperations.increment("rate-limit:/api/v1/auth/login:" + IP))
                            .willReturn(6L);
                }

                @Test
                @DisplayName("429 에러 응답을 직접 작성하고 다음 필터로 진행하지 않는다")
                void shouldWriteErrorResponseAndStopChain() throws Exception {
                    // when
                    rateLimitFilter.doFilterInternal(request, response, filterChain);

                    // then
                    verify(errorResponseWriter).send(response, AuthErrorCode.RATE_LIMIT_EXCEEDED);
                    verify(filterChain, never()).doFilter(any(), any());
                }
            }
        }
    }
}
