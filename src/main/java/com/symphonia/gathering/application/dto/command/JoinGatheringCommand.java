package com.symphonia.gathering.application.dto.command;

public record JoinGatheringCommand(
        String joinToken,
        String googleIdToken,
        String name,
        String phoneNumber,
        boolean consentAgreed,
        String consentVersion) {
    public AddParticipantCommand toAddParticipantCommand(String googleSubject) {
        return new AddParticipantCommand(
                joinToken, googleSubject, name, phoneNumber, consentAgreed, consentVersion);
    }
}
