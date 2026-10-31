package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class ShareAlreadyConfirmedException extends ConflictException {
    public ShareAlreadyConfirmedException() {
        super(GatheringErrorCode.SHARE_ALREADY_CONFIRMED);
    }
}
