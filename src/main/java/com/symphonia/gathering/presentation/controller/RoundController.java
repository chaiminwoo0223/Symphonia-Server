package com.symphonia.gathering.presentation.controller;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.application.dto.result.GatheringRoundResult;
import com.symphonia.gathering.application.usecase.AddRoundUseCase;
import com.symphonia.gathering.presentation.api.RoundApi;
import com.symphonia.gathering.presentation.dto.request.AddRoundRequest;
import com.symphonia.gathering.presentation.dto.response.GatheringRoundResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoundController implements RoundApi {
    private final AddRoundUseCase addRoundUseCase;

    @Override
    public ResponseEntity<StandardResponse<GatheringRoundResponse>> add(
            String linkToken, AddRoundRequest request) {
        GatheringRoundResult result = addRoundUseCase.add(request.toCommand(linkToken));
        GatheringRoundResponse response = GatheringRoundResponse.from(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }
}
