package com.symphonia.common.audit;

public record AuditLogEvent(String action, String outcome) {
    public static AuditLogEvent of(AuditAction action, boolean success) {
        return new AuditLogEvent(action.name(), success ? "success" : "failure");
    }
}
