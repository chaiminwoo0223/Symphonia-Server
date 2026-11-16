package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringRoundCountExceedsLimitException extends BadRequestException {
    public GatheringRoundCountExceedsLimitException() {
        super(GatheringErrorCode.GATHERING_ROUND_COUNT_EXCEEDS_LIMIT);
    }
}
