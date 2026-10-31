package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class InvalidJoinTokenException extends NotFoundException {
    public InvalidJoinTokenException() {
        super(GatheringErrorCode.INVALID_JOIN_TOKEN);
    }
}
