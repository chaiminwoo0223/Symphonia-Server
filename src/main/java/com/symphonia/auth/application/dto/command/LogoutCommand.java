package com.symphonia.auth.application.dto.command;

import com.symphonia.common.audit.HasIp;

public record LogoutCommand(String accessToken, String ip) implements HasIp {}
