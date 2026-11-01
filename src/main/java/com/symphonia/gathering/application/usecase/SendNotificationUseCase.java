package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.SendNotificationCommand;

public interface SendNotificationUseCase {
    void send(SendNotificationCommand command);
}
