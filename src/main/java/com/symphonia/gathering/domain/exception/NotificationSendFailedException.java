package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.InternalServerException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class NotificationSendFailedException extends InternalServerException {
    public NotificationSendFailedException() {
        super(GatheringErrorCode.NOTIFICATION_SEND_FAILED);
    }
}
