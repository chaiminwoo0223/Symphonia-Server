package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringOpenConflictException extends ConflictException {
    public GatheringOpenConflictException() {
        super(GatheringErrorCode.GATHERING_OPEN_CONFLICT);
    }
}
