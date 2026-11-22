package com.symphonia.gathering.application.dto.command;

public record RemoveParticipantBySubjectCommand(String joinToken, String googleSubject) {}
