package com.symphonia.gathering.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.presentation.GatheringHeaders;
import com.symphonia.gathering.presentation.ShareEndpoints;
import com.symphonia.gathering.presentation.dto.request.MarkSharesSentRequest;
import com.symphonia.gathering.presentation.dto.response.PayerSharesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Share API", description = "분담금 API")
public interface ShareApi {
    @GetMapping(ShareEndpoints.LIST)
    @Operation(summary = "분담금 조회", description = "보낼 분담금을 결제자별로 묶어 계좌, 계산 근거와 함께 조회합니다.")
    ResponseEntity<StandardResponse<List<PayerSharesResponse>>> getShares(
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken);

    @PostMapping(ShareEndpoints.SENT)
    @Operation(summary = "분담금 보냄 처리", description = "결제자에게 낼 미입금 분담금을 모두 보냄으로 바꿉니다.")
    ResponseEntity<StandardResponse<PayerSharesResponse>> markSent(
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken,
            @Valid @RequestBody MarkSharesSentRequest request);
}
