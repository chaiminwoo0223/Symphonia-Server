package com.symphonia.gathering.application.dto.command;

public record WithdrawParticipantCommand(String joinToken, String googleIdToken) {
    public RemoveParticipantBySubjectCommand toRemoveParticipantBySubjectCommand(
            String googleSubject) {
        return new RemoveParticipantBySubjectCommand(joinToken, googleSubject);
    }
}
