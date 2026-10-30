package com.symphonia.gathering.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.presentation.GatheringHeaders;
import com.symphonia.gathering.presentation.ParticipantEndpoints;
import com.symphonia.gathering.presentation.SettlementEndpoints;
import com.symphonia.gathering.presentation.dto.request.RecordSettlementRequest;
import com.symphonia.gathering.presentation.dto.request.RegisterBankAccountRequest;
import com.symphonia.gathering.presentation.dto.request.UpdateSettlementRequest;
import com.symphonia.gathering.presentation.dto.response.SettlementFormResponse;
import com.symphonia.gathering.presentation.dto.response.SettlementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Settlement API", description = "정산 API")
public interface SettlementApi {
    @GetMapping(SettlementEndpoints.FORM)
    @Operation(summary = "기록 화면 조회", description = "정산 건 기록에 필요한 참여자, 인원 기본값, 기록한 정산 건을 조회합니다.")
    ResponseEntity<StandardResponse<SettlementFormResponse>> getForm(
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken);

    @PostMapping(ParticipantEndpoints.REGISTER_BANK_ACCOUNT)
    @Operation(summary = "계좌 등록", description = "결제자의 계좌를 등록합니다. 한 번만 등록할 수 있습니다.")
    ResponseEntity<StandardResponse<Void>> registerBankAccount(
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken,
            @Valid @RequestBody RegisterBankAccountRequest request);

    @PostMapping(SettlementEndpoints.RECORD)
    @Operation(summary = "정산 건 기록", description = "정산 건을 기록하고 분담액을 계산합니다.")
    ResponseEntity<StandardResponse<SettlementResponse>> record(
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken,
            @Valid @RequestBody RecordSettlementRequest request);

    @PutMapping(SettlementEndpoints.UPDATE)
    @Operation(summary = "정산 건 수정", description = "정산 메시지를 보내기 전의 정산 건을 수정합니다.")
    ResponseEntity<StandardResponse<SettlementResponse>> update(
            @PathVariable Long settlementId,
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken,
            @Valid @RequestBody UpdateSettlementRequest request);

    @PostMapping(SettlementEndpoints.CANCEL)
    @Operation(summary = "정산 건 취소", description = "입금한 참여자가 없는 정산 건을 취소합니다.")
    ResponseEntity<StandardResponse<Void>> cancel(
            @PathVariable Long settlementId,
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken);
}
