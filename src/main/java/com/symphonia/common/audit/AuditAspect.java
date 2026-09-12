package com.symphonia.common.audit;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditLogRecorder auditLogRecorder;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String ip = resolveIp(joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            String actorId =
                    resolveActorIdFromSecurityContext().orElseGet(() -> resolveActorId(result));

            auditLogRecorder.record(audited.event(), true, ip, actorId, null);

            return result;
        } catch (Throwable throwable) {
            String actorId = resolveActorIdFromSecurityContext().orElse(null);

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

    private String resolveActorId(Object result) {
        return result instanceof HasActorId hasActorId ? hasActorId.actorId() : null;
    }

    private Optional<String> resolveActorIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        return Optional.ofNullable(authentication.getName());
    }
}
