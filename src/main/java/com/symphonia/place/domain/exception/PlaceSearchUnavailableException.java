package com.symphonia.place.domain.exception;

import com.symphonia.common.exception.ServiceUnavailableException;
import com.symphonia.place.domain.error.PlaceErrorCode;

public class PlaceSearchUnavailableException extends ServiceUnavailableException {
    public PlaceSearchUnavailableException() {
        super(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE);
    }
}
