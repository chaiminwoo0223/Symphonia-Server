package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringPairingConflictException extends ConflictException {
    public GatheringPairingConflictException() {
        super(GatheringErrorCode.GATHERING_PAIRING_CONFLICT);
    }
}
