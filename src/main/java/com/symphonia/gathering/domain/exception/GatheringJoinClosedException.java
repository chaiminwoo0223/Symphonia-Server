package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringJoinClosedException extends BadRequestException {
    public GatheringJoinClosedException() {
        super(GatheringErrorCode.GATHERING_JOIN_CLOSED);
    }
}
