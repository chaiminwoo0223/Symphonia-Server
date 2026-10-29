package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ForbiddenException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class NotGatheringHostException extends ForbiddenException {
    public NotGatheringHostException() {
        super(GatheringErrorCode.NOT_GATHERING_HOST);
    }
}
