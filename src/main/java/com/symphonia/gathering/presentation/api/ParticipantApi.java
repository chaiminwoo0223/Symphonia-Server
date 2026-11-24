package com.symphonia.gathering.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.presentation.GatheringHeaders;
import com.symphonia.gathering.presentation.ParticipantEndpoints;
import com.symphonia.gathering.presentation.dto.request.CorrectParticipantRequest;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.RecoverParticipantLinkRequest;
import com.symphonia.gathering.presentation.dto.request.SubmitParticipantPairingFeedbackRequest;
import com.symphonia.gathering.presentation.dto.request.WithdrawParticipantRequest;
import com.symphonia.gathering.presentation.dto.response.ParticipantPairingResponse;
import com.symphonia.gathering.presentation.dto.response.ParticipantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Participant API", description = "참여자 API")
public interface ParticipantApi {
    @PostMapping(ParticipantEndpoints.JOIN)
    @Operation(
            summary = "모임 참여",
            description =
                    "구글 로그인으로 본인을 확인한 뒤 모임에 참여하고 개인 링크 토큰을 발급합니다. 개인 링크 토큰은 이 응답에서만 받을 수 있습니다."
                            + " 구글 ID 토큰은 본인 확인에만 쓰고 저장하지 않으며, 같은 구글 계정으로 이미 참여했다면 409(PARTICIPANT_ALREADY_JOINED)입니다."
                            + " 참여 응답을 잃어 이 409를 받았다면 개인 링크 복구 API로 새 링크를 받습니다."
                            + " 같은 모임의 다른 참여자가 쓰는 휴대폰이면 409(PARTICIPANT_PHONE_NUMBER_ALREADY_USED)입니다."
                            + " 구글 계정과 휴대폰이 모두 겹치면 구글 계정 쪽인 409(PARTICIPANT_ALREADY_JOINED)를 줍니다."
                            + " 휴대폰 때문에 받은 409라면 먼저 오타가 없는지 확인해야 합니다."
                            + " 휴대폰 중복만으로는 기존 참여자가 구글 계정으로 참여했는지 알 수 없으므로, 그 번호로 구글 계정으로 참여한 경우에만 개인 링크 복구 API로 새 링크를 받을 수 있습니다."
                            + " 구글 계정 없이 참여한 모임장이나 이전 방식으로 참여한 참여자의 번호와 겹쳤다면 모임장이 링크를 다시 발급합니다."
                            + " 구글 ID 토큰이 없으면 400, 유효하지 않으면 400(INVALID_SOCIAL_TOKEN)입니다."
                            + " 토큰에 이메일과 이름이 없으면 400(SOCIAL_REQUIRED_INFO_MISSING)이므로 기본 로그인 범위(openid, email, profile)로 로그인해야 합니다. 이 값들은 저장하지 않습니다."
                            + " 휴대폰은 010으로 시작하는 11자리만 받으며 본인이 입력한 값이라 인증하지 않습니다. 이름은 앞뒤 공백을 제거해 저장합니다."
                            + " 카카오톡 인앱 브라우저는 구글 로그인을 막을 수 있으므로 클라이언트가 외부 브라우저로 안내합니다.")
    ResponseEntity<StandardResponse<ParticipantResponse>> join(
            @RequestHeader(GatheringHeaders.JOIN_TOKEN) String joinToken,
            @Valid @RequestBody JoinGatheringRequest request);

    @PostMapping(ParticipantEndpoints.WITHDRAWAL)
    @Operation(
            summary = "참여 철회",
            description =
                    "참여할 때 쓴 참여 토큰으로 모임을 찾고, 구글 로그인으로 본인을 확인해 참여를 철회합니다. 개인 링크는 필요하지 않고, 철회하면 개인 링크가 무효가 됩니다."
                            + " 같은 구글 계정으로 참여한 참여자가 모임에 없으면 404(PARTICIPANT_NOT_FOUND)입니다."
                            + " 취소하지 않은 정산 건의 결제자이거나 분담금 인원이면 받을 돈이 걸려 있어 409(PARTICIPANT_UNREMOVABLE)이며, 모임장이 정산 건을 고쳐 빼거나 취소한 뒤에 철회할 수 있습니다. 결제자는 정산 건에서 뺄 수 없어 취소해야 합니다."
                            + " 취소한 정산 건에만 든 참여자는 철회할 수 있고, 취소 이력은 그대로 두고 이름과 휴대폰, 구글 계정 식별값만 지웁니다."
                            + " 철회한 뒤 같은 구글 계정으로 다시 참여할 수 있습니다.")
    ResponseEntity<StandardResponse<Void>> withdraw(
            @RequestHeader(GatheringHeaders.JOIN_TOKEN) String joinToken,
            @Valid @RequestBody WithdrawParticipantRequest request);

    @PutMapping(ParticipantEndpoints.PROFILE)
    @Operation(
            summary = "참여자 이름과 휴대폰 정정",
            description =
                    "참여할 때 쓴 참여 토큰으로 모임을 찾고, 구글 로그인으로 본인을 확인해 이름과 휴대폰을 정정합니다. 개인 링크는 필요하지 않고 개인 링크는 바뀌지 않습니다."
                            + " 이름과 휴대폰을 모두 보내야 하며, 현재 값과 같아도 됩니다. 204입니다."
                            + " 모임의 파기 시각 전까지는 참여 기간이 끝난 뒤에도 정정할 수 있고, 정산 건에 든 참여자도 정정할 수 있습니다."
                            + " 정산 화면은 이름과 휴대폰을 매번 새로 읽으므로 정정한 값이 바로 보이며 금액과 분담금 상태는 바뀌지 않습니다."
                            + " 이미 정산 화면을 본 결제자는 이전 값을 볼 수 있습니다."
                            + " 휴대폰은 010으로 시작하는 11자리만 받으며 본인이 입력한 값이라 인증하지 않습니다. 이름은 앞뒤 공백을 제거해 저장합니다."
                            + " 같은 모임의 다른 참여자가 쓰는 휴대폰이면 409(PARTICIPANT_PHONE_NUMBER_ALREADY_USED)입니다."
                            + " 구글 ID 토큰이 없으면 400, 유효하지 않으면 400(INVALID_SOCIAL_TOKEN)입니다."
                            + " 참여 토큰을 찾지 못하거나 파기된 모임이면 404(JOIN_TOKEN_NOT_FOUND)입니다."
                            + " 이 구글 계정으로 참여한 참여자가 없으면 404(PARTICIPANT_NOT_FOUND)이며, 구글 계정 없이 참여한 모임장과 이전 방식으로 참여한 참여자는 정정할 수 없습니다.")
    ResponseEntity<StandardResponse<Void>> correctProfile(
            @RequestHeader(GatheringHeaders.JOIN_TOKEN) String joinToken,
            @Valid @RequestBody CorrectParticipantRequest request);

    @PostMapping(ParticipantEndpoints.LINK_TOKEN)
    @Operation(
            summary = "개인 링크 복구",
            description =
                    "개인 링크를 잃은 참여자가 참여할 때 쓴 구글 계정으로 다시 로그인해 새 개인 링크 토큰을 발급받습니다. 새 토큰은 201 응답에서만 받을 수 있습니다."
                            + " 새 토큰을 발급하면 이전 개인 링크는 모두 무효가 됩니다. 모임은 참여 토큰으로 찾으며 모임장이 참여 QR을 재발급했다면 새 QR의 토큰으로 요청해야 합니다. 모임의 파기 시각 전까지는 참여 기간이 끝난 뒤에도 복구할 수 있습니다."
                            + " 구글 ID 토큰이 없으면 400, 유효하지 않으면 400(INVALID_SOCIAL_TOKEN)입니다."
                            + " 참여 토큰을 찾지 못하거나 파기된 모임이면 404(JOIN_TOKEN_NOT_FOUND)입니다."
                            + " 이 구글 계정으로 참여한 참여자가 없으면 404(PARTICIPANT_NOT_FOUND)이며, 구글 계정 없이 참여한 모임장과 이전 방식으로 참여한 참여자는 모임장이 링크를 다시 발급합니다.")
    ResponseEntity<StandardResponse<ParticipantResponse>> recoverLinkToken(
            @RequestHeader(GatheringHeaders.JOIN_TOKEN) String joinToken,
            @Valid @RequestBody RecoverParticipantLinkRequest request);

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
