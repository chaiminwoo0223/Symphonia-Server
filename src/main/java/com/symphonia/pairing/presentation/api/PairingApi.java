package com.symphonia.pairing.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.pairing.presentation.dto.request.RecommendPairingRequest;
import com.symphonia.pairing.presentation.dto.request.SubmitPairingFeedbackRequest;
import com.symphonia.pairing.presentation.dto.response.PairingFeedbackResponse;
import com.symphonia.pairing.presentation.dto.response.PairingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/pairings")
@Tag(name = "Pairing API", description = "술, 안주, 음악 페어링 추천 API")
public interface PairingApi {
    @GetMapping("/recommend")
    @Operation(
            summary = "페어링 추천",
            description = "관계 유형과 분위기, 성인 인증 여부, 참석자 음주 제약과 알레르기 정보를 기반으로 술, 안주, 음악 조합을 추천합니다.")
    ResponseEntity<StandardResponse<List<PairingResponse>>> recommend(
            @Valid @ModelAttribute RecommendPairingRequest request);

    @PostMapping("/feedback")
    @Operation(summary = "페어링 피드백 기록", description = "인증된 멤버가 선택한 페어링 조합을 기록합니다.")
    ResponseEntity<StandardResponse<PairingFeedbackResponse>> submitFeedback(
            @AuthenticationPrincipal String memberId,
            @Valid @RequestBody SubmitPairingFeedbackRequest request);
}
