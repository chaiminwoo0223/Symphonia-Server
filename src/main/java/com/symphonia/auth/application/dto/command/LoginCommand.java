package com.symphonia.auth.application.dto.command;

import com.symphonia.common.audit.HasIp;

public record LoginCommand(String provider, String code, String ip) implements HasIp {}
