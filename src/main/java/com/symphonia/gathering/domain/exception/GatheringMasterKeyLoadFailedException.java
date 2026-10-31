package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.InternalServerException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringMasterKeyLoadFailedException extends InternalServerException {
    public GatheringMasterKeyLoadFailedException() {
        super(GatheringErrorCode.GATHERING_MASTER_KEY_LOAD_FAILED);
    }
}
