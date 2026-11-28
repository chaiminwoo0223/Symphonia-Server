package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class UnpaidShareConflictException extends ConflictException {
    public UnpaidShareConflictException() {
        super(GatheringErrorCode.UNPAID_SHARE_CONFLICT);
    }
}
