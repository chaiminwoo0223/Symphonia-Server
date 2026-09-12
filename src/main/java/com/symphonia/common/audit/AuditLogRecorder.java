package com.symphonia.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogRecorder {
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");

    private final ObjectMapper objectMapper;

    public void record(
            AuditAction action, boolean success, String ip, String actorId, String detail) {
        AuditLogEntry entry = AuditLogEntry.of(action, success, ip, actorId, detail);

        AUDIT_LOGGER.info(toJson(entry));
    }

    private String toJson(AuditLogEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (JacksonException e) {
            log.warn("감사 로그 JSON 직렬화에 실패했습니다.", e);

            return entry.toString();
        }
    }
}
