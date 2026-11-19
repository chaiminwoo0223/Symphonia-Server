package com.symphonia.gathering.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.presentation.GatheringHeaders;
import com.symphonia.gathering.presentation.ParticipantEndpoints;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.SubmitParticipantPairingFeedbackRequest;
import com.symphonia.gathering.presentation.dto.response.ParticipantPairingResponse;
import com.symphonia.gathering.presentation.dto.response.ParticipantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Participant API", description = "참여자 API")
public interface ParticipantApi {
    @PostMapping(ParticipantEndpoints.JOIN)
    @Operation(
            summary = "모임 참여",
            description = "모임에 참여하고 개인 링크 토큰을 발급합니다. 개인 링크 토큰은 이 응답에서만 받을 수 있습니다.")
    ResponseEntity<StandardResponse<ParticipantResponse>> join(
            @RequestHeader(GatheringHeaders.JOIN_TOKEN) String joinToken,
            @Valid @RequestBody JoinGatheringRequest request);

    @GetMapping(ParticipantEndpoints.PAIRING)
    @Operation(
            summary = "모임 페어링 조회",
            description = "모임에서 고른 페어링과 피드백을 남겼는지, 지금 남길 수 있는지를 조회합니다. 고른 페어링이 없으면 404를 반환합니다.")
    ResponseEntity<StandardResponse<ParticipantPairingResponse>> getPairing(
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken);

    @PostMapping(ParticipantEndpoints.PAIRING_FEEDBACK)
    @Operation(
            summary = "페어링 피드백 남기기",
            description =
                    "모임에서 고른 페어링에 익명 피드백을 남깁니다. 참여자마다 한 번만, 분담금을 보내거나 받은 뒤에 남길 수 있습니다."
                            + " 요청한 술과 안주가 모임의 페어링과 다르면 409를 반환하니 모임 페어링 조회를 다시 불러와 주세요.")
    ResponseEntity<StandardResponse<Void>> submitPairingFeedback(
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken,
            @Valid @RequestBody SubmitParticipantPairingFeedbackRequest request);
}
