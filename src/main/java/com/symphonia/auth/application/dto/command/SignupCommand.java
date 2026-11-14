package com.symphonia.auth.application.dto.command;

import com.symphonia.common.audit.HasIp;

public record SignupCommand(String provider, String token, String ip) implements HasIp {}
