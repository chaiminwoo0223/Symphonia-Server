package com.symphonia.gathering.presentation.controller;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.application.dto.command.CancelSettlementCommand;
import com.symphonia.gathering.application.dto.command.ReissueJoinTokenCommand;
import com.symphonia.gathering.application.dto.command.ReissueParticipantLinkTokenCommand;
import com.symphonia.gathering.application.dto.command.RemoveParticipantCommand;
import com.symphonia.gathering.application.dto.query.GetGatheringParticipantsQuery;
import com.symphonia.gathering.application.dto.query.GetSettlementFormQuery;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;
import com.symphonia.gathering.application.dto.result.GatheringParticipantResult;
import com.symphonia.gathering.application.dto.result.GatheringResult;
import com.symphonia.gathering.application.dto.result.GatheringRoundResult;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.usecase.AddRoundUseCase;
import com.symphonia.gathering.application.usecase.CancelSettlementUseCase;
import com.symphonia.gathering.application.usecase.ChooseGatheringPairingUseCase;
import com.symphonia.gathering.application.usecase.GetGatheringParticipantsUseCase;
import com.symphonia.gathering.application.usecase.GetGatheringUseCase;
import com.symphonia.gathering.application.usecase.GetSettlementFormUseCase;
import com.symphonia.gathering.application.usecase.OpenGatheringUseCase;
import com.symphonia.gathering.application.usecase.RecordSettlementUseCase;
import com.symphonia.gathering.application.usecase.ReissueJoinTokenUseCase;
import com.symphonia.gathering.application.usecase.ReissueParticipantLinkTokenUseCase;
import com.symphonia.gathering.application.usecase.RemoveParticipantUseCase;
import com.symphonia.gathering.application.usecase.UpdateSettlementUseCase;
import com.symphonia.gathering.presentation.api.GatheringApi;
import com.symphonia.gathering.presentation.dto.request.AddRoundRequest;
import com.symphonia.gathering.presentation.dto.request.GatheringPairingRequest;
import com.symphonia.gathering.presentation.dto.request.OpenGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.RecordSettlementRequest;
import com.symphonia.gathering.presentation.dto.request.UpdateSettlementRequest;
import com.symphonia.gathering.presentation.dto.response.GatheringJoinTokenResponse;
import com.symphonia.gathering.presentation.dto.response.GatheringParticipantResponse;
import com.symphonia.gathering.presentation.dto.response.GatheringResponse;
import com.symphonia.gathering.presentation.dto.response.GatheringRoundResponse;
import com.symphonia.gathering.presentation.dto.response.ParticipantResponse;
import com.symphonia.gathering.presentation.dto.response.SettlementFormResponse;
import com.symphonia.gathering.presentation.dto.response.SettlementResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GatheringController implements GatheringApi {
    private final OpenGatheringUseCase openGatheringUseCase;
    private final ReissueJoinTokenUseCase reissueJoinTokenUseCase;
    private final ChooseGatheringPairingUseCase chooseGatheringPairingUseCase;
    private final AddRoundUseCase addRoundUseCase;
    private final GetSettlementFormUseCase getSettlementFormUseCase;
    private final RecordSettlementUseCase recordSettlementUseCase;
    private final UpdateSettlementUseCase updateSettlementUseCase;
    private final CancelSettlementUseCase cancelSettlementUseCase;
    private final GetGatheringUseCase getGatheringUseCase;
    private final GetGatheringParticipantsUseCase getGatheringParticipantsUseCase;
    private final RemoveParticipantUseCase removeParticipantUseCase;
    private final ReissueParticipantLinkTokenUseCase reissueParticipantLinkTokenUseCase;

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
    public ResponseEntity<StandardResponse<Void>> choosePairing(
            String memberId, Long gatheringId, GatheringPairingRequest request) {
        chooseGatheringPairingUseCase.choose(request.toChooseCommand(memberId, gatheringId));

        return ResponseEntity.status(HttpStatus.OK).body(StandardResponse.success(HttpStatus.OK));
    }

    @Override
    public ResponseEntity<StandardResponse<GatheringRoundResponse>> addRound(
            String memberId, Long gatheringId, AddRoundRequest request) {
        GatheringRoundResult result =
                addRoundUseCase.add(request.toCommand(Long.parseLong(memberId), gatheringId));
        GatheringRoundResponse response = GatheringRoundResponse.from(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }

    @Override
    public ResponseEntity<StandardResponse<SettlementFormResponse>> getSettlementForm(
            String memberId, Long gatheringId) {
        SettlementFormResult result =
                getSettlementFormUseCase.getForm(
                        GetSettlementFormQuery.of(Long.parseLong(memberId), gatheringId));
        SettlementFormResponse response = SettlementFormResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<SettlementResponse>> recordSettlement(
            String memberId, Long gatheringId, RecordSettlementRequest request) {
        SettlementResult result =
                recordSettlementUseCase.record(
                        request.toCommand(Long.parseLong(memberId), gatheringId));
        SettlementResponse response = SettlementResponse.from(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(HttpStatus.CREATED, response));
    }

    @Override
    public ResponseEntity<StandardResponse<SettlementResponse>> updateSettlement(
            String memberId, Long gatheringId, Long settlementId, UpdateSettlementRequest request) {
        SettlementResult result =
                updateSettlementUseCase.update(
                        request.toCommand(Long.parseLong(memberId), gatheringId, settlementId));
        SettlementResponse response = SettlementResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> cancelSettlement(
            String memberId, Long gatheringId, Long settlementId) {
        cancelSettlementUseCase.cancel(
                CancelSettlementCommand.of(Long.parseLong(memberId), gatheringId, settlementId));

        return ResponseEntity.status(HttpStatus.OK).body(StandardResponse.success(HttpStatus.OK));
    }

    @Override
    public ResponseEntity<StandardResponse<List<GatheringParticipantResponse>>> getParticipants(
            String memberId, Long gatheringId) {
        List<GatheringParticipantResult> results =
                getGatheringParticipantsUseCase.getParticipants(
                        GetGatheringParticipantsQuery.of(Long.parseLong(memberId), gatheringId));
        List<GatheringParticipantResponse> response =
                results.stream().map(GatheringParticipantResponse::from).toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> removeParticipant(
            String memberId, Long gatheringId, Long participantId) {
        removeParticipantUseCase.remove(
                RemoveParticipantCommand.of(Long.parseLong(memberId), gatheringId, participantId));

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(StandardResponse.success(HttpStatus.NO_CONTENT));
    }

    @Override
    public ResponseEntity<StandardResponse<ParticipantResponse>> reissueLinkToken(
            String memberId, Long gatheringId, Long participantId) {
        ParticipantResult result =
                reissueParticipantLinkTokenUseCase.reissue(
                        ReissueParticipantLinkTokenCommand.of(
                                Long.parseLong(memberId), gatheringId, participantId));
        ParticipantResponse response = ParticipantResponse.from(result);

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
