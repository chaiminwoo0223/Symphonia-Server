package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringNotFoundException extends NotFoundException {
    public GatheringNotFoundException() {
        super(GatheringErrorCode.GATHERING_NOT_FOUND);
    }
}
