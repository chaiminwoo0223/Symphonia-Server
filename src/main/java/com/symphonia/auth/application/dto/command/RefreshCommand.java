package com.symphonia.auth.application.dto.command;

import com.symphonia.common.audit.HasIp;

public record RefreshCommand(String refreshToken, String ip) implements HasIp {}
