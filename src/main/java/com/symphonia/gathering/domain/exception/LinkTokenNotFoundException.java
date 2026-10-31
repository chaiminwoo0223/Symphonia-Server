package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.NotFoundException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class LinkTokenNotFoundException extends NotFoundException {
    public LinkTokenNotFoundException() {
        super(GatheringErrorCode.LINK_TOKEN_NOT_FOUND);
    }
}
