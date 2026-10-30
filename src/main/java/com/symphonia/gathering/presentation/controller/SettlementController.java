package com.symphonia.gathering.presentation.controller;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.application.dto.command.CancelSettlementCommand;
import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.usecase.CancelSettlementUseCase;
import com.symphonia.gathering.application.usecase.GetSettlementFormUseCase;
import com.symphonia.gathering.application.usecase.RecordSettlementUseCase;
import com.symphonia.gathering.application.usecase.UpdateSettlementUseCase;
import com.symphonia.gathering.presentation.api.SettlementApi;
import com.symphonia.gathering.presentation.dto.request.RecordSettlementRequest;
import com.symphonia.gathering.presentation.dto.request.UpdateSettlementRequest;
import com.symphonia.gathering.presentation.dto.response.SettlementFormResponse;
import com.symphonia.gathering.presentation.dto.response.SettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SettlementController implements SettlementApi {
    private final GetSettlementFormUseCase getSettlementFormUseCase;
    private final RecordSettlementUseCase recordSettlementUseCase;
    private final UpdateSettlementUseCase updateSettlementUseCase;
    private final CancelSettlementUseCase cancelSettlementUseCase;

    @Override
    public ResponseEntity<StandardResponse<SettlementFormResponse>> getForm(String linkToken) {
        SettlementFormResult result = getSettlementFormUseCase.getForm(linkToken);
        SettlementFormResponse response = SettlementFormResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<SettlementResponse>> record(
            String linkToken, RecordSettlementRequest request) {
        SettlementResult result = recordSettlementUseCase.record(request.toCommand(linkToken));
        SettlementResponse response = SettlementResponse.from(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }

    @Override
    public ResponseEntity<StandardResponse<SettlementResponse>> update(
            Long settlementId, String linkToken, UpdateSettlementRequest request) {
        SettlementResult result =
                updateSettlementUseCase.update(request.toCommand(linkToken, settlementId));
        SettlementResponse response = SettlementResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> cancel(Long settlementId, String linkToken) {
        cancelSettlementUseCase.cancel(CancelSettlementCommand.of(linkToken, settlementId));

        return ResponseEntity.status(HttpStatus.OK).body(StandardResponse.success(HttpStatus.OK));
    }
}
