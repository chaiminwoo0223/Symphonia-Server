package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.result.PayerCollectionResult;

public interface GetPayerCollectionUseCase {
    PayerCollectionResult getCollection(String linkToken);
}
