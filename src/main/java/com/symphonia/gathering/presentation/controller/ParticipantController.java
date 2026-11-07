package com.symphonia.gathering.presentation.controller;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.application.dto.result.ParticipantPairingResult;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.usecase.GetParticipantPairingUseCase;
import com.symphonia.gathering.application.usecase.JoinGatheringUseCase;
import com.symphonia.gathering.application.usecase.RegisterBankAccountUseCase;
import com.symphonia.gathering.application.usecase.SubmitParticipantPairingFeedbackUseCase;
import com.symphonia.gathering.presentation.api.ParticipantApi;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.RegisterBankAccountRequest;
import com.symphonia.gathering.presentation.dto.request.SubmitParticipantPairingFeedbackRequest;
import com.symphonia.gathering.presentation.dto.response.ParticipantPairingResponse;
import com.symphonia.gathering.presentation.dto.response.ParticipantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ParticipantController implements ParticipantApi {
    private final JoinGatheringUseCase joinGatheringUseCase;
    private final RegisterBankAccountUseCase registerBankAccountUseCase;
    private final GetParticipantPairingUseCase getParticipantPairingUseCase;
    private final SubmitParticipantPairingFeedbackUseCase submitParticipantPairingFeedbackUseCase;

    @Override
    public ResponseEntity<StandardResponse<ParticipantResponse>> join(
            String joinToken, JoinGatheringRequest request) {
        ParticipantResult result = joinGatheringUseCase.join(request.toCommand(joinToken));
        ParticipantResponse response = ParticipantResponse.from(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> registerBankAccount(
            String linkToken, RegisterBankAccountRequest request) {
        registerBankAccountUseCase.register(request.toCommand(linkToken));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED));
    }

    @Override
    public ResponseEntity<StandardResponse<ParticipantPairingResponse>> getPairing(
            String linkToken) {
        ParticipantPairingResult result = getParticipantPairingUseCase.getPairing(linkToken);
        ParticipantPairingResponse response = ParticipantPairingResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> submitPairingFeedback(
            String linkToken, SubmitParticipantPairingFeedbackRequest request) {
        submitParticipantPairingFeedbackUseCase.submit(request.toCommand(linkToken));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED));
    }
}
