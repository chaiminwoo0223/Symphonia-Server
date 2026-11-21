package com.symphonia.pairing.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.pairing.presentation.PairingEndpoints;
import com.symphonia.pairing.presentation.dto.request.RecommendPairingRequest;
import com.symphonia.pairing.presentation.dto.request.SubmitPairingFeedbackRequest;
import com.symphonia.pairing.presentation.dto.response.PairingFeedbackResponse;
import com.symphonia.pairing.presentation.dto.response.PairingRecommendationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Pairing API", description = "페어링 API")
public interface PairingApi {
    @GetMapping(PairingEndpoints.RECOMMEND)
    @Operation(
            summary = "페어링 추천",
            description =
                    "관계 유형과 분위기, 성인 인증 여부, 참석자 음주 제약과 알레르기 정보를 기반으로 술과 안주 조합을 추천합니다. "
                            + "로그인한 회원이 요청하면 그 회원이 싫어요를 남긴 조합은 결과에서 제외합니다. "
                            + "액세스 토큰이 없거나 유효하지 않으면 비로그인 요청으로 처리합니다. "
                            + "도수 제한이나 알레르기 때문에 추천이 비었거나 요청한 무알코올 음료를 찾지 못하면 notices에 사유를 담습니다.")
    ResponseEntity<StandardResponse<PairingRecommendationResponse>> recommend(
            @AuthenticationPrincipal(expression = "#this == 'anonymousUser' ? null : #this")
                    String memberId,
            @Valid @ModelAttribute RecommendPairingRequest request);

    @PostMapping(PairingEndpoints.FEEDBACK)
    @Operation(
            summary = "페어링 피드백 기록",
            description = "인증된 멤버가 선택한 페어링 조합을 기록합니다. 같은 조합에 다시 내면 기존 피드백을 바꾸고 200을 응답합니다.")
    ResponseEntity<StandardResponse<PairingFeedbackResponse>> submitFeedback(
            @AuthenticationPrincipal String memberId,
            @Valid @RequestBody SubmitPairingFeedbackRequest request);
}
