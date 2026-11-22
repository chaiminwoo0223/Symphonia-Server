package com.symphonia.gathering.application.dto.command;

public record CorrectParticipantCommand(
        String joinToken, String googleIdToken, String name, String phoneNumber) {
    public CorrectParticipantByGoogleSubjectCommand toCorrectByGoogleSubjectCommand(
            String googleSubject) {
        return new CorrectParticipantByGoogleSubjectCommand(
                joinToken, googleSubject, name, phoneNumber);
    }
}
