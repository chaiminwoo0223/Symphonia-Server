package com.symphonia.gathering.application.dto.command;

public record CorrectParticipantByGoogleSubjectCommand(
        String joinToken, String googleSubject, String name, String phoneNumber) {}
