package com.symphonia.common.audit;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class AuditLogRecorder {
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");

    private final ObjectMapper objectMapper;

    public void record(
            AuditEvent event, boolean success, String ip, String actorId, String detail) {
        AuditLogEntry entry =
                new AuditLogEntry(
                        new AuditLogEntry.Event(event.name(), success ? "success" : "failure"),
                        new AuditLogEntry.Source(ip),
                        new AuditLogEntry.User(actorId),
                        detail);

        AUDIT_LOGGER.info(toJson(entry));
    }

    private String toJson(AuditLogEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (JacksonException e) {
            return entry.toString();
        }
    }

    private record AuditLogEntry(Event event, Source source, User user, String detail) {
        record Event(String action, String outcome) {}

        record Source(String ip) {}

        record User(String id) {}
    }
}
