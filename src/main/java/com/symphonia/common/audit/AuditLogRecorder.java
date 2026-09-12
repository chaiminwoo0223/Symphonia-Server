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
            AuditEvent event, boolean success, String ip, String actorId, String detail) {
        AuditLogEntry entry = AuditLogEntry.of(event, success, ip, actorId, detail);

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

    private record AuditLogEntry(Event event, Source source, User user, String detail) {
        private static AuditLogEntry of(
                AuditEvent event, boolean success, String ip, String actorId, String detail) {
            return new AuditLogEntry(
                    Event.of(event, success), new Source(ip), new User(actorId), detail);
        }

        private record Event(String action, String outcome) {
            private static Event of(AuditEvent event, boolean success) {
                return new Event(event.name(), success ? "success" : "failure");
            }
        }

        private record Source(String ip) {}

        private record User(String id) {}
    }
}
