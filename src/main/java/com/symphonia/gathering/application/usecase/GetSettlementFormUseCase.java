package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.query.GetSettlementFormQuery;
import com.symphonia.gathering.application.dto.result.SettlementFormResult;

public interface GetSettlementFormUseCase {
    SettlementFormResult getForm(GetSettlementFormQuery query);
}
