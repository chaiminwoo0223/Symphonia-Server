package com.symphonia.gathering.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.presentation.GatheringEndpoints;
import com.symphonia.gathering.presentation.GatheringHeaders;
import com.symphonia.gathering.presentation.ParticipantEndpoints;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.OpenGatheringRequest;
import com.symphonia.gathering.presentation.dto.response.GatheringJoinTokenResponse;
import com.symphonia.gathering.presentation.dto.response.GatheringResponse;
import com.symphonia.gathering.presentation.dto.response.ParticipantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Gathering API", description = "모임 API")
public interface GatheringApi {
    @PostMapping(GatheringEndpoints.OPEN)
    @Operation(summary = "모임 열기", description = "모임을 열고 참여 토큰을 발급합니다.")
    ResponseEntity<StandardResponse<GatheringJoinTokenResponse>> open(
            @AuthenticationPrincipal String memberId,
            @Valid @RequestBody OpenGatheringRequest request);

    @PostMapping(GatheringEndpoints.REISSUE)
    @Operation(summary = "참여 토큰 재발급", description = "참여 토큰을 재발급합니다. 기존 참여 토큰은 무효가 됩니다.")
    ResponseEntity<StandardResponse<GatheringJoinTokenResponse>> reissue(
            @AuthenticationPrincipal String memberId, @PathVariable Long gatheringId);

    @GetMapping(GatheringEndpoints.PREVIEW)
    @Operation(summary = "참여 화면 조회", description = "모임 이름과 참여 가능 여부를 조회합니다.")
    ResponseEntity<StandardResponse<GatheringResponse>> preview(
            @RequestHeader(GatheringHeaders.JOIN_TOKEN) String joinToken);

    @PostMapping(ParticipantEndpoints.JOIN)
    @Operation(
            summary = "모임 참여",
            description = "모임에 참여하고 개인 링크 토큰을 발급합니다. 개인 링크 토큰은 이 응답에서만 받을 수 있습니다.")
    ResponseEntity<StandardResponse<ParticipantResponse>> join(
            @RequestHeader(GatheringHeaders.JOIN_TOKEN) String joinToken,
            @Valid @RequestBody JoinGatheringRequest request);
}
