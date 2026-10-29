package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.InternalServerException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringDataKeyNotFoundException extends InternalServerException {
    public GatheringDataKeyNotFoundException() {
        super(GatheringErrorCode.GATHERING_DATA_KEY_NOT_FOUND);
    }
}
