package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class InvalidGatheringRoundNumberException extends BadRequestException {
    public InvalidGatheringRoundNumberException() {
        super(GatheringErrorCode.INVALID_GATHERING_ROUND_NUMBER);
    }
}
