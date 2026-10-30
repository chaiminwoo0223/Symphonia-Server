package com.symphonia.gathering.presentation.controller;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.application.dto.command.ReissueJoinTokenCommand;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;
import com.symphonia.gathering.application.dto.result.GatheringResult;
import com.symphonia.gathering.application.usecase.GetGatheringUseCase;
import com.symphonia.gathering.application.usecase.OpenGatheringUseCase;
import com.symphonia.gathering.application.usecase.ReissueJoinTokenUseCase;
import com.symphonia.gathering.presentation.api.GatheringApi;
import com.symphonia.gathering.presentation.dto.request.OpenGatheringRequest;
import com.symphonia.gathering.presentation.dto.response.GatheringJoinTokenResponse;
import com.symphonia.gathering.presentation.dto.response.GatheringResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GatheringController implements GatheringApi {
    private final OpenGatheringUseCase openGatheringUseCase;
    private final ReissueJoinTokenUseCase reissueJoinTokenUseCase;
    private final GetGatheringUseCase getGatheringUseCase;

    @Override
    public ResponseEntity<StandardResponse<GatheringJoinTokenResponse>> open(
            String memberId, OpenGatheringRequest request) {
        GatheringJoinTokenResult result = openGatheringUseCase.open(request.toCommand(memberId));
        GatheringJoinTokenResponse response = GatheringJoinTokenResponse.from(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }

    @Override
    public ResponseEntity<StandardResponse<GatheringJoinTokenResponse>> reissue(
            String memberId, Long gatheringId) {
        ReissueJoinTokenCommand command =
                ReissueJoinTokenCommand.of(Long.parseLong(memberId), gatheringId);
        GatheringJoinTokenResult result = reissueJoinTokenUseCase.reissue(command);
        GatheringJoinTokenResponse response = GatheringJoinTokenResponse.from(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }

    @Override
    public ResponseEntity<StandardResponse<GatheringResponse>> preview(String joinToken) {
        GatheringResult result = getGatheringUseCase.getByJoinToken(joinToken);
        GatheringResponse response = GatheringResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }
}
