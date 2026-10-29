package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.InternalServerException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class InvalidGatheringMasterKeyException extends InternalServerException {
    public InvalidGatheringMasterKeyException() {
        super(GatheringErrorCode.INVALID_GATHERING_MASTER_KEY);
    }
}
