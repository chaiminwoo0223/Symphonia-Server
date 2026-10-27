package com.symphonia.catalog.domain.exception;

import com.symphonia.catalog.domain.error.CatalogErrorCode;
import com.symphonia.common.exception.NotFoundException;

public class MusicMoodNotFoundException extends NotFoundException {
    public MusicMoodNotFoundException() {
        super(CatalogErrorCode.MUSIC_MOOD_NOT_FOUND);
    }
}
