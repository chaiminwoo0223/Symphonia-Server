package com.symphonia.gathering.domain.exception;

import com.symphonia.common.exception.ConflictException;
import com.symphonia.gathering.domain.error.GatheringErrorCode;

public class ShareStatusChangeNotAllowedException extends ConflictException {
    public ShareStatusChangeNotAllowedException() {
        super(GatheringErrorCode.SHARE_STATUS_CHANGE_NOT_ALLOWED);
    }
}
