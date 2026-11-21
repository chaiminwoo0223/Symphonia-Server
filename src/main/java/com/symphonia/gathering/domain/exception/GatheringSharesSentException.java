package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringSharesSentException extends ConflictException {
    public GatheringSharesSentException() {
        super(GatheringErrorCode.GATHERING_SHARES_SENT);
    }
}
