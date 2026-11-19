package com.symphonia.gathering.presentation.controller;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.application.dto.command.ConfirmShareCommand;
import com.symphonia.gathering.application.dto.command.RevertShareCommand;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.usecase.ConfirmShareUseCase;
import com.symphonia.gathering.application.usecase.RevertShareUseCase;
import com.symphonia.gathering.presentation.api.SettlementApi;
import com.symphonia.gathering.presentation.dto.response.SettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SettlementController implements SettlementApi {
    private final ConfirmShareUseCase confirmShareUseCase;
    private final RevertShareUseCase revertShareUseCase;

    @Override
    public ResponseEntity<StandardResponse<SettlementResponse>> confirmShare(
            Long settlementId, Long participantId, String linkToken) {
        SettlementResult result =
                confirmShareUseCase.confirm(
                        ConfirmShareCommand.of(linkToken, settlementId, participantId));
        SettlementResponse response = SettlementResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<SettlementResponse>> revertShare(
            Long settlementId, Long participantId, String linkToken) {
        SettlementResult result =
                revertShareUseCase.revert(
                        RevertShareCommand.of(linkToken, settlementId, participantId));
        SettlementResponse response = SettlementResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }
}
