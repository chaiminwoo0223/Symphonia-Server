package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class PairingFeedbackAlreadySubmittedException extends ConflictException {
    public PairingFeedbackAlreadySubmittedException() {
        super(GatheringErrorCode.PAIRING_FEEDBACK_ALREADY_SUBMITTED);
    }
}
