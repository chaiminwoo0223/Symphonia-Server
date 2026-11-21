package com.symphonia.gathering.application.dto.command;

public record AddParticipantCommand(
        String joinToken,
        String googleSubject,
        String name,
        String phoneNumber,
        boolean consentAgreed,
        String consentVersion) {}
