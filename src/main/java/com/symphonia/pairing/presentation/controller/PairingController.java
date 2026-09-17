package com.symphonia.pairing.presentation.controller;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.pairing.application.dto.result.PairingFeedbackResult;
import com.symphonia.pairing.application.dto.result.PairingResult;
import com.symphonia.pairing.application.usecase.RecommendPairingUseCase;
import com.symphonia.pairing.application.usecase.SubmitPairingFeedbackUseCase;
import com.symphonia.pairing.presentation.api.PairingApi;
import com.symphonia.pairing.presentation.dto.request.RecommendPairingRequest;
import com.symphonia.pairing.presentation.dto.request.SubmitPairingFeedbackRequest;
import com.symphonia.pairing.presentation.dto.response.PairingFeedbackResponse;
import com.symphonia.pairing.presentation.dto.response.PairingResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PairingController implements PairingApi {
    private final RecommendPairingUseCase recommendPairingUseCase;
    private final SubmitPairingFeedbackUseCase submitPairingFeedbackUseCase;

    @Override
    public ResponseEntity<StandardResponse<List<PairingResponse>>> recommend(
            RecommendPairingRequest request) {
        List<PairingResult> results = recommendPairingUseCase.recommend(request.toQuery());
        List<PairingResponse> response = results.stream().map(PairingResponse::from).toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<PairingFeedbackResponse>> submitFeedback(
            String memberId, SubmitPairingFeedbackRequest request) {
        PairingFeedbackResult result =
                submitPairingFeedbackUseCase.submit(request.toCommand(memberId));
        PairingFeedbackResponse response = PairingFeedbackResponse.from(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }
}
