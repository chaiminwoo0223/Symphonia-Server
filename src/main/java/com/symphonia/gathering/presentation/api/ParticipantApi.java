package com.symphonia.gathering.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.presentation.GatheringHeaders;
import com.symphonia.gathering.presentation.ParticipantEndpoints;
import com.symphonia.gathering.presentation.dto.request.JoinGatheringRequest;
import com.symphonia.gathering.presentation.dto.request.RegisterBankAccountRequest;
import com.symphonia.gathering.presentation.dto.response.ParticipantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

    @PostMapping(ParticipantEndpoints.REGISTER_BANK_ACCOUNT)
    @Operation(summary = "계좌 등록", description = "결제자의 계좌를 등록합니다. 한 번만 등록할 수 있습니다.")
    ResponseEntity<StandardResponse<Void>> registerBankAccount(
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken,
            @Valid @RequestBody RegisterBankAccountRequest request);
}
