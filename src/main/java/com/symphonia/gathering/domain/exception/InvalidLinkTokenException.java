package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class InvalidLinkTokenException extends NotFoundException {
    public InvalidLinkTokenException() {
        super(GatheringErrorCode.INVALID_LINK_TOKEN);
    }
}
