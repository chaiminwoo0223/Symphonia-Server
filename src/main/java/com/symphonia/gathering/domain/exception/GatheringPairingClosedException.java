package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class GatheringPairingClosedException extends BadRequestException {
    public GatheringPairingClosedException() {
        super(GatheringErrorCode.GATHERING_PAIRING_CLOSED);
    }
}
