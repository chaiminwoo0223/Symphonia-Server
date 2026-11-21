package com.symphonia.gathering.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.presentation.GatheringEndpoints;
import com.symphonia.gathering.presentation.GatheringHeaders;
import com.symphonia.gathering.presentation.dto.request.AddRoundRequest;
import com.symphonia.gathering.presentation.dto.request.GatheringPairingRequest;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.OpenGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.RecordSettlementRequest;
import com.symphonia.gathering.presentation.dto.request.UpdateSettlementRequest;
import com.symphonia.gathering.presentation.dto.response.GatheringJoinTokenResponse;
import com.symphonia.gathering.presentation.dto.response.GatheringParticipantResponse;
import com.symphonia.gathering.presentation.dto.response.GatheringResponse;
import com.symphonia.gathering.presentation.dto.response.GatheringRoundResponse;
import com.symphonia.gathering.presentation.dto.response.HostedGatheringResponse;
import com.symphonia.gathering.presentation.dto.response.ParticipantResponse;
import com.symphonia.gathering.presentation.dto.response.RecordingExtensionResponse;
import com.symphonia.gathering.presentation.dto.response.SettlementFormResponse;
import com.symphonia.gathering.presentation.dto.response.SettlementResponse;
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

    @GetMapping(GatheringEndpoints.LIST)
    @Operation(
            summary = "내 모임 목록",
            description =
                    "내가 연 모임을 최근에 연 순서로 돌려줍니다. 개인정보 파기 시각이 지난 모임은 담지 않습니다. 참여자는 이름과 휴대폰을 읽지 않고 수만 담습니다. 목록은 페이지로 나누지 않습니다. 파기 시각이 지나면 사라지고 모임은 최대 29일만 남으므로 길이가 제한됩니다.")
    ResponseEntity<StandardResponse<List<HostedGatheringResponse>>> getHosted(
            @AuthenticationPrincipal String memberId);

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
            description =
                    "참여자에게 피드백을 물을 페어링을 고르거나 바꿉니다. 기록 기간(모임을 연 뒤 48시간, 연장하면 96시간) 안에만 할 수 있습니다.")
    ResponseEntity<StandardResponse<Void>> choosePairing(
            @AuthenticationPrincipal String memberId,
            @PathVariable Long gatheringId,
            @Valid @RequestBody GatheringPairingRequest request);

    @PostMapping(GatheringEndpoints.ADD_ROUND)
    @Operation(
            summary = "차수 추가",
            description =
                    "모임에 다음 차수를 추가합니다. 모임을 만든 회원만 할 수 있고, 기록 기간(모임을 연 뒤 48시간, 연장하면 96시간) 안에만 할 수 있습니다. 요청한 번호의 차수가 이미 있으면 새로 만들지 않고 그 차수를 돌려줍니다. 마지막 차수의 다음 번호까지만 추가할 수 있습니다.")
    ResponseEntity<StandardResponse<GatheringRoundResponse>> addRound(
            @AuthenticationPrincipal String memberId,
            @PathVariable Long gatheringId,
            @Valid @RequestBody AddRoundRequest request);

    @PutMapping(GatheringEndpoints.EXTEND_RECORDING)
    @Operation(
            summary = "기록 기간 연장",
            description =
                    """
                    기록 기간을 48시간 연장합니다. 모임을 만든 회원만 할 수 있고, 모임마다 한 번만 연장할 수 있습니다. 연장하면 정산 건 기록, 차수 추가, 참여, 페어링 변경을 할 수 있는 기간이 함께 늘어납니다. 개인정보 파기 시각은 연장한 기록 기간을 기준으로 다시 계산되며, 모임을 연 뒤 29일을 넘지 않습니다.

                    기록 기간이 끝나기 전에만 연장할 수 있고, 끝난 모임은 400(GATHERING_RECORDING_EXTENSION_CLOSED)을 반환합니다. 이미 연장한 모임에 다시 요청하면 바꾸지 않고 연장한 기간이 끝나는 시각을 돌려줍니다. 기록 화면 조회의 recordingExtended로 연장 버튼을 보일지 정할 수 있습니다.
                    """)
    ResponseEntity<StandardResponse<RecordingExtensionResponse>> extendRecording(
            @AuthenticationPrincipal String memberId, @PathVariable Long gatheringId);

    @GetMapping(GatheringEndpoints.SETTLEMENT_FORM)
    @Operation(
            summary = "기록 화면 조회",
            description =
                    "정산 건 기록에 필요한 차수, 참여자, 인원 기본값, 기록한 정산 건을 조회합니다. 모임을 만든 회원만 할 수 있습니다. 인원 기본값은 직전에 기록한 정산 건의 인원이고, 첫 건이면 참여자 전원입니다.")
    ResponseEntity<StandardResponse<SettlementFormResponse>> getSettlementForm(
            @AuthenticationPrincipal String memberId, @PathVariable Long gatheringId);

    @PostMapping(GatheringEndpoints.SETTLEMENTS)
    @Operation(
            summary = "정산 건 기록",
            description =
                    "모임장이 정산 건을 기록하고 분담액을 계산합니다. 결제자는 payerParticipantId로 지정합니다. 같은 결제자와 같은 요청 id로 다시 보내면 새로 만들지 않고 처음 기록한 정산 건을 돌려줍니다. 요청 id가 같아도 결제자가 다르면 다른 요청으로 보고 새 정산 건을 만듭니다.")
    ResponseEntity<StandardResponse<SettlementResponse>> recordSettlement(
            @AuthenticationPrincipal String memberId,
            @PathVariable Long gatheringId,
            @Valid @RequestBody RecordSettlementRequest request);

    @PutMapping(GatheringEndpoints.SETTLEMENT)
    @Operation(
            summary = "정산 건 수정",
            description = "돈을 보낸 참여자가 없는 정산 건을 수정합니다. 결제자는 바꿀 수 없으니, 결제자를 바꾸려면 취소하고 다시 기록합니다.")
    ResponseEntity<StandardResponse<SettlementResponse>> updateSettlement(
            @AuthenticationPrincipal String memberId,
            @PathVariable Long gatheringId,
            @PathVariable Long settlementId,
            @Valid @RequestBody UpdateSettlementRequest request);

    @PostMapping(GatheringEndpoints.CANCEL_SETTLEMENT)
    @Operation(summary = "정산 건 취소", description = "입금한 참여자가 없는 정산 건을 취소합니다. 기록 기간이 지나도 취소할 수 있습니다.")
    ResponseEntity<StandardResponse<Void>> cancelSettlement(
            @AuthenticationPrincipal String memberId,
            @PathVariable Long gatheringId,
            @PathVariable Long settlementId);

    @GetMapping(GatheringEndpoints.PARTICIPANTS)
    @Operation(
            summary = "참여자 목록 조회",
            description = "모임의 참여자를 참여한 순서대로 조회합니다. 휴대폰 번호는 마스킹하며, 개인 링크 토큰은 담지 않습니다.")
    ResponseEntity<StandardResponse<List<GatheringParticipantResponse>>> getParticipants(
            @AuthenticationPrincipal String memberId, @PathVariable Long gatheringId);

    @PostMapping(GatheringEndpoints.PARTICIPANTS)
    @Operation(
            summary = "모임장 참여",
            description =
                    """
                    모임을 만든 회원이 참여 QR 없이 로그인한 채로 참여자가 됩니다. 모임장이 결제자로 지정되거나 정산 건 인원에 들어가려면 먼저 참여자여야 합니다. 응답의 linkToken은 모임장이 결제자일 때 수금 화면을 볼 개인 링크 토큰이며, 이 응답에서만 받을 수 있습니다.

                    참여 기간, 동의, 휴대폰 중복, 참여자 상한은 참여 QR로 참여할 때와 같은 규칙입니다. 기록 기간이 끝났으면 400(GATHERING_JOIN_CLOSED), 이미 참여한 휴대폰이면 409(PARTICIPANT_ALREADY_JOINED)입니다.

                    참여자가 된 모임장이 다른 결제자의 정산 건 인원에 들어가면 모임장의 이름과 휴대폰 번호가 그 결제자에게 전달되고, 분담 금액도 그 결제자에게 보입니다. 동의 안내문은 참여 화면과 같은 내용을 보여 주고, consentVersion에는 기록 화면 조회 응답의 값을 그대로 보내야 합니다.
                    """)
    ResponseEntity<StandardResponse<ParticipantResponse>> joinAsHost(
            @AuthenticationPrincipal String memberId,
            @PathVariable Long gatheringId,
            @Valid @RequestBody JoinGatheringRequest request);

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

                    참여 화면의 동의 안내문에는 이름과 휴대폰 번호가 정산 건의 결제자에게 전달된다는 목적(카카오톡 방 초대)과 이름이 같은 차수의 참여자에게 보인다는 목적을 담아야 합니다. 안내문을 바꾸면 consentVersion이 바뀌므로, 참여 요청에는 이 응답의 consentVersion을 그대로 보내야 합니다.
                    """)
    ResponseEntity<StandardResponse<GatheringResponse>> preview(
            @RequestHeader(GatheringHeaders.JOIN_TOKEN) String joinToken);
}
