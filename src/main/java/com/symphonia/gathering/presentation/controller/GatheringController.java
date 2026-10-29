package com.symphonia.gathering.presentation.controller;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.application.dto.command.ReissueJoinTokenCommand;
import com.symphonia.gathering.application.dto.result.GatheringJoinInfoResult;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.usecase.CreateGatheringUseCase;
import com.symphonia.gathering.application.usecase.GetGatheringJoinInfoUseCase;
import com.symphonia.gathering.application.usecase.JoinGatheringUseCase;
import com.symphonia.gathering.application.usecase.ReissueJoinTokenUseCase;
import com.symphonia.gathering.presentation.api.GatheringApi;
import com.symphonia.gathering.presentation.dto.request.CreateGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
import com.symphonia.gathering.presentation.dto.response.GatheringJoinInfoResponse;
import com.symphonia.gathering.presentation.dto.response.GatheringJoinTokenResponse;
import com.symphonia.gathering.presentation.dto.response.ParticipantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GatheringController implements GatheringApi {
    private final CreateGatheringUseCase createGatheringUseCase;
    private final ReissueJoinTokenUseCase reissueJoinTokenUseCase;
    private final GetGatheringJoinInfoUseCase getGatheringJoinInfoUseCase;
    private final JoinGatheringUseCase joinGatheringUseCase;

    @Override
    public ResponseEntity<StandardResponse<GatheringJoinTokenResponse>> create(
            String memberId, CreateGatheringRequest request) {
        GatheringJoinTokenResult result =
                createGatheringUseCase.create(request.toCommand(memberId));
        GatheringJoinTokenResponse response = GatheringJoinTokenResponse.from(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }

    @Override
    public ResponseEntity<StandardResponse<GatheringJoinTokenResponse>> reissueJoinToken(
            String memberId, Long gatheringId) {
        ReissueJoinTokenCommand command =
                ReissueJoinTokenCommand.of(Long.parseLong(memberId), gatheringId);
        GatheringJoinTokenResult result = reissueJoinTokenUseCase.reissue(command);
        GatheringJoinTokenResponse response = GatheringJoinTokenResponse.from(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }

    @Override
    public ResponseEntity<StandardResponse<GatheringJoinInfoResponse>> getJoinInfo(
            String joinToken) {
        GatheringJoinInfoResult result = getGatheringJoinInfoUseCase.getJoinInfo(joinToken);
        GatheringJoinInfoResponse response = GatheringJoinInfoResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<ParticipantResponse>> join(
            String joinToken, JoinGatheringRequest request) {
        ParticipantResult result = joinGatheringUseCase.join(request.toCommand(joinToken));
        ParticipantResponse response = ParticipantResponse.from(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }
}
