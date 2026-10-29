package com.symphonia.gathering.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.presentation.GatheringEndpoints;
import com.symphonia.gathering.presentation.dto.request.CreateGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
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

@Tag(name = "Gathering API", description = "술자리 모임 생성과 참여 API")
public interface GatheringApi {
    @PostMapping(GatheringEndpoints.GATHERINGS)
    @Operation(summary = "모임 생성", description = "인증된 멤버가 모임을 만들고 참여 토큰을 발급받습니다.")
    ResponseEntity<StandardResponse<GatheringJoinTokenResponse>> create(
            @AuthenticationPrincipal String memberId,
            @Valid @RequestBody CreateGatheringRequest request);

    @PostMapping(GatheringEndpoints.JOIN_TOKEN)
    @Operation(
            summary = "참여 토큰 재발급",
            description = "모임을 만든 멤버가 참여 토큰을 다시 발급받습니다. 기존 참여 토큰은 무효가 됩니다.")
    ResponseEntity<StandardResponse<GatheringJoinTokenResponse>> reissueJoinToken(
            @AuthenticationPrincipal String memberId, @PathVariable Long gatheringId);

    @GetMapping(GatheringEndpoints.JOIN)
    @Operation(summary = "참여 화면 조회", description = "참여 토큰으로 모임 이름, 참여 가능 여부, 동의 안내 버전을 조회합니다.")
    ResponseEntity<StandardResponse<GatheringResponse>> getByJoinToken(
            @PathVariable String joinToken);

    @PostMapping(GatheringEndpoints.JOIN)
    @Operation(
            summary = "모임 참여",
            description = "참여 토큰으로 모임에 참여하고 개인 링크 토큰을 발급받습니다. 개인 링크 토큰은 이 응답에서만 받을 수 있습니다.")
    ResponseEntity<StandardResponse<ParticipantResponse>> join(
            @PathVariable String joinToken, @Valid @RequestBody JoinGatheringRequest request);
}
