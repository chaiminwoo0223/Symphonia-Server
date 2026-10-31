package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class ShareAlreadyUnpaidException extends ConflictException {
    public ShareAlreadyUnpaidException() {
        super(GatheringErrorCode.SHARE_ALREADY_UNPAID);
    }
}
