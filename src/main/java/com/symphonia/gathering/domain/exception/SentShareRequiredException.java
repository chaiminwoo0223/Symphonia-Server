package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.BadRequestException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class SentShareRequiredException extends BadRequestException {
    public SentShareRequiredException() {
        super(GatheringErrorCode.SENT_SHARE_REQUIRED);
    }
}
