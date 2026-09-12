package com.symphonia.common.audit;

public record AuditLogEvent(String action, String outcome) {
    public static AuditLogEvent of(AuditEvent event, boolean success) {
        return new AuditLogEvent(event.name(), success ? "success" : "failure");
    }
}
