package com.symphonia.gathering.application.dto.command;

public record JoinGatheringCommand(
        String joinToken,
        String googleIdToken,
        String name,
        String phoneNumber,
        boolean consentAgreed,
        String consentVersion) {
    public JoinWithVerifiedSubjectCommand toVerifiedSubjectCommand(String googleSubject) {
        return new JoinWithVerifiedSubjectCommand(
                joinToken, googleSubject, name, phoneNumber, consentAgreed, consentVersion);
    }
}
