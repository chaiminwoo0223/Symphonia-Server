package com.symphonia.gathering.application.dto.command;

public record JoinWithVerifiedSubjectCommand(
        String joinToken,
        String googleSubject,
        String name,
        String phoneNumber,
        boolean consentAgreed,
        String consentVersion) {}
