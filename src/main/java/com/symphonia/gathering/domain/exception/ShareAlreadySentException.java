package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class ShareAlreadySentException extends ConflictException {
    public ShareAlreadySentException() {
        super(GatheringErrorCode.SHARE_ALREADY_SENT);
    }
}
