package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.result.SharesResult;

public interface GetSharesUseCase {
    SharesResult getShares(String linkToken);
}
