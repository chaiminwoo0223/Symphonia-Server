package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import java.util.List;

public interface GetSharesUseCase {
    List<PayerSharesResult> getShares(String linkToken);
}
