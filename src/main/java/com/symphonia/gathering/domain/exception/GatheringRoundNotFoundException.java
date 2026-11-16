package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringRoundNotFoundException extends NotFoundException {
    public GatheringRoundNotFoundException() {
        super(GatheringErrorCode.GATHERING_ROUND_NOT_FOUND);
    }
}
