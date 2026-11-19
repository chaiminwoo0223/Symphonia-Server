package com.symphonia.gathering.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.presentation.GatheringEndpoints;
import com.symphonia.gathering.presentation.GatheringHeaders;
import com.symphonia.gathering.presentation.dto.request.GatheringPairingRequest;
import com.symphonia.gathering.presentation.dto.request.OpenGatheringRequest;
import com.symphonia.gathering.presentation.dto.response.GatheringJoinTokenResponse;
import com.symphonia.gathering.presentation.dto.response.GatheringParticipantResponse;
import com.symphonia.gathering.presentation.dto.response.GatheringResponse;
import com.symphonia.gathering.presentation.dto.response.ParticipantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    @Operation(
            summary = "참여 토큰 재발급",
            description =
                    """
                    참여 토큰을 재발급합니다. 기존 참여 토큰은 무효가 됩니다. 응답의 expiresAt은 이 토큰으로 참여할 수 있는 기간이 끝나는 시각입니다.

                    같은 모임에 재발급을 동시에 여러 번 호출하면 마지막에 처리된 토큰만 유효합니다. 서버는 이를 직렬화하지 않으므로, 클라이언트가 재발급 버튼을 누르는 동안 중복 호출을 막고 마지막 응답의 토큰으로 QR을 그려야 합니다.
                    """)
    ResponseEntity<StandardResponse<GatheringJoinTokenResponse>> reissue(
            @AuthenticationPrincipal String memberId, @PathVariable Long gatheringId);

    @PutMapping(GatheringEndpoints.CHOOSE_PAIRING)
    @Operation(
            summary = "모임 페어링 고르기",
            description = "참여자에게 피드백을 물을 페어링을 고르거나 바꿉니다. 기록 기간(모임을 연 뒤 48시간) 안에만 할 수 있습니다.")
    ResponseEntity<StandardResponse<Void>> choosePairing(
            @AuthenticationPrincipal String memberId,
            @PathVariable Long gatheringId,
            @Valid @RequestBody GatheringPairingRequest request);

    @GetMapping(GatheringEndpoints.PARTICIPANTS)
    @Operation(
            summary = "참여자 목록 조회",
            description = "모임의 참여자를 참여한 순서대로 조회합니다. 휴대폰 번호는 마스킹하며, 개인 링크 토큰은 담지 않습니다.")
    ResponseEntity<StandardResponse<List<GatheringParticipantResponse>>> getParticipants(
            @AuthenticationPrincipal String memberId, @PathVariable Long gatheringId);

    @DeleteMapping(GatheringEndpoints.PARTICIPANT)
    @Operation(
            summary = "참여자 제거",
            description =
                    """
                    참여자를 제거합니다. 그 참여자의 개인 링크는 무효가 됩니다.

                    정산 건의 결제자이거나 분담금 인원인 참여자는 취소한 정산 건까지 포함해 제거할 수 없고 409(PARTICIPANT_UNREMOVABLE)를 반환합니다. 제거한 휴대폰도 참여 QR로 다시 참여할 수 있으니, 유출된 QR은 참여 QR 재발급으로 막아야 합니다.
                    """)
    ResponseEntity<StandardResponse<Void>> removeParticipant(
            @AuthenticationPrincipal String memberId,
            @PathVariable Long gatheringId,
            @PathVariable Long participantId);

    @PostMapping(GatheringEndpoints.REISSUE_LINK_TOKEN)
    @Operation(
            summary = "개인 링크 재발급",
            description =
                    "링크를 잃은 참여자의 개인 링크 토큰을 재발급합니다. 그 참여자의 기존 개인 링크 토큰은 모두 무효가 되고, 새 토큰은 이 응답에서만 받을 수 있습니다.")
    ResponseEntity<StandardResponse<ParticipantResponse>> reissueLinkToken(
            @AuthenticationPrincipal String memberId,
            @PathVariable Long gatheringId,
            @PathVariable Long participantId);

    @GetMapping(GatheringEndpoints.PREVIEW)
    @Operation(
            summary = "참여 화면 조회",
            description =
                    """
                    모임 이름과 참여 가능 여부를 조회합니다. 참여할 수 없으면 joinClosedReason으로 사유(PERIOD_EXPIRED, PARTICIPANT_LIMIT_REACHED)를 알려 줍니다.

                    참여 화면에서 입력한 값(이름, 휴대폰 번호, 동의 여부)의 유지는 서버가 아니라 클라이언트가 처리합니다. 서버는 입력 중인 값을 저장하지 않습니다.
                    """)
    ResponseEntity<StandardResponse<GatheringResponse>> preview(
            @RequestHeader(GatheringHeaders.JOIN_TOKEN) String joinToken);
}
