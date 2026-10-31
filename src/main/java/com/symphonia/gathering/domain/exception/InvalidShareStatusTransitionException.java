package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class InvalidShareStatusTransitionException extends ConflictException {
    public InvalidShareStatusTransitionException() {
        super(GatheringErrorCode.INVALID_SHARE_STATUS_TRANSITION);
    }
}
