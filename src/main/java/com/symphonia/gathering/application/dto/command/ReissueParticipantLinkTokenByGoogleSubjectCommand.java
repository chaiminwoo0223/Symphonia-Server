package com.symphonia.gathering.application.dto.command;

public record ReissueParticipantLinkTokenByGoogleSubjectCommand(
        String joinToken, String googleSubject) {}
