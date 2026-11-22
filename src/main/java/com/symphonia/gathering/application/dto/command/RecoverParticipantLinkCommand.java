package com.symphonia.gathering.application.dto.command;

public record RecoverParticipantLinkCommand(String joinToken, String googleIdToken) {
    public ReissueParticipantLinkTokenByGoogleSubjectCommand toReissueCommand(
            String googleSubject) {
        return new ReissueParticipantLinkTokenByGoogleSubjectCommand(joinToken, googleSubject);
    }
}
