package com.symphonia.auth.application.dto.command;

public record LoginCommand(String provider, String code) {}
