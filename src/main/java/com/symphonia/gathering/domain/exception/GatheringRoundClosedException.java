package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringRoundClosedException extends BadRequestException {
    public GatheringRoundClosedException() {
        super(GatheringErrorCode.GATHERING_ROUND_CLOSED);
    }
}
