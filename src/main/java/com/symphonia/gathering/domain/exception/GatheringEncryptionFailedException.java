package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.InternalServerException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringEncryptionFailedException extends InternalServerException {
    public GatheringEncryptionFailedException() {
        super(GatheringErrorCode.GATHERING_ENCRYPTION_FAILED);
    }
}
