package com.symphonia.common.audit;

public record AuditLogEntry(
        AuditLogEvent event, AuditLogSource source, AuditLogUser user, String detail) {
    public static AuditLogEntry of(
            AuditEvent event, boolean success, String ip, String actorId, String detail) {
        return new AuditLogEntry(
                AuditLogEvent.of(event, success),
                new AuditLogSource(ip),
                new AuditLogUser(actorId),
                detail);
    }
}
