package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringPairingNotFoundException extends NotFoundException {
    public GatheringPairingNotFoundException() {
        super(GatheringErrorCode.GATHERING_PAIRING_NOT_FOUND);
    }
}
