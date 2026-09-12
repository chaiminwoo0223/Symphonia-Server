package com.symphonia.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("AuditAspect 단위 테스트")
class AuditAspectTest extends UnitTest {

    private static final String IP = "127.0.0.1";

    @InjectMocks private AuditAspect auditAspect;

    @Mock private AuditLogRecorder auditLogRecorder;

    @Mock private ActorIdResolver actorIdResolver;

    @Mock private ProceedingJoinPoint joinPoint;

    @Mock private Audited audited;

    private record Command(String ip) implements HasIp {}

    private record Result(String actorId) implements HasActorId {}

    @Nested
    @DisplayName("audit 메서드는")
    class Audit {

        @BeforeEach
        void setUp() {
            given(audited.event()).willReturn(AuditEvent.LOGIN);
            given(joinPoint.getArgs()).willReturn(new Object[] {new Command(IP)});
        }

        @Nested
        @DisplayName("반환값이 HasActorId를 구현하는 경우 (로그인/가입/재발급 성공)")
        class WhenResultHasActorId {

            @BeforeEach
            void setUp() throws Throwable {
                given(joinPoint.proceed()).willReturn(new Result("new-actor"));
            }

            @Test
            @DisplayName("ActorIdResolver를 조회하지 않고 반환값의 actorId로 성공 감사 로그를 남긴다")
            void shouldRecordSuccessUsingResultActorIdWithoutConsultingResolver() throws Throwable {
                // when
                Object result = auditAspect.audit(joinPoint, audited);

                // then
                then(auditLogRecorder)
                        .should()
                        .record(AuditEvent.LOGIN, true, IP, "new-actor", null);
                then(actorIdResolver).shouldHaveNoInteractions();
                assertThat(result).isEqualTo(new Result("new-actor"));
            }
        }

        @Nested
        @DisplayName("반환값이 HasActorId를 구현하지 않는 경우 (로그아웃처럼 void인 흐름)")
        class WhenResultDoesNotHaveActorId {

            @BeforeEach
            void setUp() throws Throwable {
                given(joinPoint.proceed()).willReturn(null);
            }

            @Test
            @DisplayName("ActorIdResolver가 신원을 찾으면 그 값으로 성공 감사 로그를 남긴다")
            void shouldFallBackToActorIdResolverWhenPresent() throws Throwable {
                // given
                given(actorIdResolver.resolve()).willReturn(Optional.of("context-actor"));

                // when
                auditAspect.audit(joinPoint, audited);

                // then
                then(auditLogRecorder)
                        .should()
                        .record(AuditEvent.LOGIN, true, IP, "context-actor", null);
            }

            @Test
            @DisplayName("ActorIdResolver도 신원을 못 찾으면 actorId 없이 성공 감사 로그를 남긴다")
            void shouldRecordNullActorIdWhenResolverEmpty() throws Throwable {
                // given
                given(actorIdResolver.resolve()).willReturn(Optional.empty());

                // when
                auditAspect.audit(joinPoint, audited);

                // then
                then(auditLogRecorder).should().record(AuditEvent.LOGIN, true, IP, null, null);
            }
        }

        @Nested
        @DisplayName("대상 메서드가 예외를 던진 경우")
        class WhenTargetMethodThrows {

            private final RuntimeException exception = new RuntimeException("실패 원인");

            @BeforeEach
            void setUp() throws Throwable {
                given(joinPoint.proceed()).willThrow(exception);
                given(actorIdResolver.resolve()).willReturn(Optional.empty());
            }

            @Test
            @DisplayName("ActorIdResolver로 actorId를 채워 실패 감사 로그를 남기고 예외를 그대로 전파한다")
            void shouldRecordFailureAndPropagateException() {
                // when & then
                assertThatThrownBy(() -> auditAspect.audit(joinPoint, audited)).isSameAs(exception);
                then(auditLogRecorder).should().record(AuditEvent.LOGIN, false, IP, null, "실패 원인");
            }
        }
    }
}
