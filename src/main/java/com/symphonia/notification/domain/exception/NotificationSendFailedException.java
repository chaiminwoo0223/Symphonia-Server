package com.symphonia.notification.domain.exception;

import com.symphonia.common.exception.InternalServerException;
import com.symphonia.notification.domain.error.NotificationErrorCode;

public class NotificationSendFailedException extends InternalServerException {
    public NotificationSendFailedException() {
        super(NotificationErrorCode.NOTIFICATION_SEND_FAILED);
    }
}
