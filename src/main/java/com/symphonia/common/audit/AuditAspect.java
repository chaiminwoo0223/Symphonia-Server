package com.symphonia.common.audit;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditLogRecorder auditLogRecorder;
    private final ActorIdResolver actorIdResolver;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String ip = resolveIp(joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            String actorId = resolveActorId(result).orElseGet(this::resolveActorIdFromContext);

            auditLogRecorder.record(audited.event(), true, ip, actorId, null);

            return result;
        } catch (Throwable throwable) {
            String actorId = resolveActorIdFromContext();

            auditLogRecorder.record(audited.event(), false, ip, actorId, throwable.getMessage());

            throw throwable;
        }
    }

    private String resolveIp(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof HasIp hasIp) {
                return hasIp.ip();
            }
        }

        return null;
    }

    // 결과가 새로 발급된 신원(HasActorId)을 담고 있으면 그것이 우선이다.
    // 요청 시점에 이미 인증돼 있던 신원(actorIdResolver)은 로그인/가입/재발급처럼
    // 결과가 곧 신원을 바꾸는 흐름에서는 낡은 값일 수 있어, 결과에 신원이 없을 때만 fallback한다.
    private Optional<String> resolveActorId(Object result) {
        return result instanceof HasActorId hasActorId
                ? Optional.ofNullable(hasActorId.actorId())
                : Optional.empty();
    }

    private String resolveActorIdFromContext() {
        return actorIdResolver.resolve().orElse(null);
    }
}
