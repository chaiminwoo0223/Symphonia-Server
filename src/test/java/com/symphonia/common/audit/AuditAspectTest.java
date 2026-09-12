package com.symphonia.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("AuditAspect 단위 테스트")
class AuditAspectTest extends UnitTest {

    private static final String IP = "127.0.0.1";

    @InjectMocks private AuditAspect auditAspect;

    @Mock private AuditLogRecorder auditLogRecorder;

    @Mock private ProceedingJoinPoint joinPoint;

    @Mock private Audited audited;

    private record Command(String ip) implements HasIp {}

    private record Result(String actorId) implements HasActorId {}

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("audit 메서드는")
    class Audit {

        @BeforeEach
        void setUp() {
            given(audited.event()).willReturn(AuditEvent.LOGIN);
            given(joinPoint.getArgs()).willReturn(new Object[] {new Command(IP)});
        }

        @Nested
        @DisplayName("대상 메서드가 정상적으로 반환된 경우")
        class WhenTargetMethodSucceeds {

            @Nested
            @DisplayName("인증된 SecurityContext가 없는 경우")
            class WhenSecurityContextAbsent {

                @BeforeEach
                void setUp() throws Throwable {
                    given(joinPoint.proceed()).willReturn(new Result("member-1"));
                }

                @Test
                @DisplayName("반환값의 actorId로 성공 감사 로그를 남기고 반환값을 그대로 돌려준다")
                void shouldRecordSuccessWithActorIdFromResult() throws Throwable {
                    // when
                    Object result = auditAspect.audit(joinPoint, audited);

                    // then
                    then(auditLogRecorder)
                            .should()
                            .record(AuditEvent.LOGIN, true, IP, "member-1", null);
                    assertThat(result).isEqualTo(new Result("member-1"));
                }
            }

            @Nested
            @DisplayName("인증된 SecurityContext가 있는 경우")
            class WhenSecurityContextPresent {

                @BeforeEach
                void setUp() throws Throwable {
                    SecurityContextHolder.getContext()
                            .setAuthentication(
                                    new UsernamePasswordAuthenticationToken(
                                            "member-2",
                                            "access-token",
                                            java.util.List.of(
                                                    new SimpleGrantedAuthority("ROLE_MEMBER"))));
                    given(joinPoint.proceed()).willReturn(new Result("member-1"));
                }

                @Test
                @DisplayName("반환값이 아닌 SecurityContext의 principal을 actorId로 기록한다")
                void shouldRecordSuccessWithActorIdFromSecurityContext() throws Throwable {
                    // when
                    auditAspect.audit(joinPoint, audited);

                    // then
                    then(auditLogRecorder)
                            .should()
                            .record(AuditEvent.LOGIN, true, IP, "member-2", null);
                }
            }

            @Nested
            @DisplayName("SecurityContext가 익명 인증인 경우")
            class WhenSecurityContextAnonymous {

                @BeforeEach
                void setUp() throws Throwable {
                    SecurityContextHolder.getContext()
                            .setAuthentication(
                                    new AnonymousAuthenticationToken(
                                            "key",
                                            "anonymousUser",
                                            java.util.List.of(
                                                    new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
                    given(joinPoint.proceed()).willReturn(new Result("member-1"));
                }

                @Test
                @DisplayName("익명 인증을 무시하고 반환값의 actorId로 기록한다")
                void shouldIgnoreAnonymousAuthentication() throws Throwable {
                    // when
                    auditAspect.audit(joinPoint, audited);

                    // then
                    then(auditLogRecorder)
                            .should()
                            .record(AuditEvent.LOGIN, true, IP, "member-1", null);
                }
            }
        }

        @Nested
        @DisplayName("대상 메서드가 예외를 던진 경우")
        class WhenTargetMethodThrows {

            private final RuntimeException exception = new RuntimeException("실패 원인");

            @BeforeEach
            void setUp() throws Throwable {
                given(joinPoint.proceed()).willThrow(exception);
            }

            @Test
            @DisplayName("actorId 없이 실패 감사 로그를 남기고 예외를 그대로 전파한다")
            void shouldRecordFailureAndPropagateException() {
                // when & then
                assertThatThrownBy(() -> auditAspect.audit(joinPoint, audited)).isSameAs(exception);
                then(auditLogRecorder).should().record(AuditEvent.LOGIN, false, IP, null, "실패 원인");
            }
        }
    }
}
