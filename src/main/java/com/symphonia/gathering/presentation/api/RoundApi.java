package com.symphonia.gathering.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.gathering.presentation.GatheringHeaders;
import com.symphonia.gathering.presentation.RoundEndpoints;
import com.symphonia.gathering.presentation.dto.request.AddRoundRequest;
import com.symphonia.gathering.presentation.dto.response.GatheringRoundResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Round API", description = "차수 API")
public interface RoundApi {
    @PostMapping(RoundEndpoints.ADD)
    @Operation(
            summary = "차수 추가",
            description =
                    "모임에 다음 차수를 추가합니다. 요청한 번호의 차수가 이미 있으면 새로 만들지 않고 그 차수를 돌려줍니다. 마지막 차수의 다음 번호까지만 추가할 수 있습니다.")
    ResponseEntity<StandardResponse<GatheringRoundResponse>> add(
            @RequestHeader(GatheringHeaders.LINK_TOKEN) String linkToken,
            @Valid @RequestBody AddRoundRequest request);
}
