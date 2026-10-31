package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class JoinTokenNotFoundException extends NotFoundException {
    public JoinTokenNotFoundException() {
        super(GatheringErrorCode.JOIN_TOKEN_NOT_FOUND);
    }
}
