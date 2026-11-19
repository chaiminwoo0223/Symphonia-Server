package com.symphonia.gathering.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.presentation.GatheringHeaders;
import com.symphonia.gathering.presentation.SettlementEndpoints;
import com.symphonia.gathering.presentation.dto.response.SettlementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Settlement API", description = "정산 API")
public interface SettlementApi {
    @PostMapping(SettlementEndpoints.CONFIRM_SHARE)
    @Operation(summary = "분담금 확인 완료", description = "참여자가 보낸 돈이나 직접 받은 돈을 확인 완료로 처리합니다.")
    ResponseEntity<StandardResponse<SettlementResponse>> confirmShare(
            @PathVariable Long settlementId,
            @PathVariable Long participantId,
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken);

    @PostMapping(SettlementEndpoints.REVERT_SHARE)
    @Operation(summary = "분담금 미입금 되돌리기", description = "아직 들어오지 않은 분담금을 미입금으로 되돌립니다.")
    ResponseEntity<StandardResponse<SettlementResponse>> revertShare(
            @PathVariable Long settlementId,
            @PathVariable Long participantId,
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken);
}
