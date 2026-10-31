package com.symphonia.gathering.presentation.controller;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import com.symphonia.gathering.application.usecase.GetSharesUseCase;
import com.symphonia.gathering.application.usecase.MarkSharesSentUseCase;
import com.symphonia.gathering.presentation.api.ShareApi;
import com.symphonia.gathering.presentation.dto.request.MarkSharesSentRequest;
import com.symphonia.gathering.presentation.dto.response.PayerSharesResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShareController implements ShareApi {
    private final GetSharesUseCase getSharesUseCase;
    private final MarkSharesSentUseCase markSharesSentUseCase;

    @Override
    public ResponseEntity<StandardResponse<List<PayerSharesResponse>>> getShares(String linkToken) {
        List<PayerSharesResult> results = getSharesUseCase.getShares(linkToken);
        List<PayerSharesResponse> response =
                results.stream().map(PayerSharesResponse::from).toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<PayerSharesResponse>> markSent(
            String linkToken, MarkSharesSentRequest request) {
        PayerSharesResult result = markSharesSentUseCase.markSent(request.toCommand(linkToken));
        PayerSharesResponse response = PayerSharesResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }
}
