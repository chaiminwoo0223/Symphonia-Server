package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class ZeroShareStatusUnchangeableException extends ConflictException {
    public ZeroShareStatusUnchangeableException() {
        super(GatheringErrorCode.ZERO_SHARE_STATUS_UNCHANGEABLE);
    }
}
