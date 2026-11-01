package com.symphonia.notification.application.usecase;

import com.symphonia.notification.application.dto.command.SendNotificationCommand;

public interface SendNotificationUseCase {
    void send(SendNotificationCommand command);
}
