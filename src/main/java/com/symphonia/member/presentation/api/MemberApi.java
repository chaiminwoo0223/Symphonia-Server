package com.symphonia.member.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.member.presentation.MemberEndpoints;
import com.symphonia.member.presentation.dto.request.UpdateMemberRequest;
import com.symphonia.member.presentation.dto.response.MemberResponse;
import com.symphonia.member.presentation.dto.response.UpdateMemberResponse;
import com.symphonia.member.presentation.dto.response.WithdrawalCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Member API", description = "멤버 API")
public interface MemberApi {
    @GetMapping(MemberEndpoints.ME)
    @Operation(summary = "멤버 조회", description = "인증된 멤버의 정보를 조회합니다.")
    ResponseEntity<StandardResponse<MemberResponse>> get(@AuthenticationPrincipal String memberId);

    @PatchMapping(MemberEndpoints.ME)
    @Operation(summary = "멤버 수정", description = "인증된 멤버의 정보를 수정합니다.")
    ResponseEntity<StandardResponse<UpdateMemberResponse>> update(
            @AuthenticationPrincipal String memberId,
            @Valid @RequestBody UpdateMemberRequest request);

    @GetMapping(MemberEndpoints.WITHDRAWAL_CHECK)
    @Operation(
            summary = "탈퇴 전 확인",
            description =
                    """
                    탈퇴하기 전에 진행 중인 모임을 확인합니다. 회원이 모임장이고 개인정보 파기 시각이 지나지 않은 모임의 수와, 그 모임들의 파기 예정 시각 중 가장 늦은 시각을 돌려줍니다.

                    탈퇴해도 모임은 파기 시각까지 남고 참여자는 분담금 확인을 이어갈 수 있습니다. 다만 탈퇴한 모임장은 새 정산 건 기록, 차수 추가, 참여 QR과 개인 링크 재발급, 참여자 관리를 할 수 없고, 같은 소셜 계정으로 다시 가입해도 모임을 되찾을 수 없습니다. 진행 중인 모임이 있으면 클라이언트가 이 내용을 안내한 뒤 탈퇴를 진행해야 합니다.
                    """)
    ResponseEntity<StandardResponse<WithdrawalCheckResponse>> checkWithdrawal(
            @AuthenticationPrincipal String memberId);

    @DeleteMapping(MemberEndpoints.ME)
    @Operation(
            summary = "멤버 삭제",
            description = "인증된 멤버를 삭제합니다. 진행 중인 모임이 있는지는 탈퇴 전 확인 API로 미리 확인할 수 있습니다.")
    ResponseEntity<StandardResponse<Void>> delete(
            @AuthenticationPrincipal String memberId,
            Authentication authentication,
            HttpServletRequest httpRequest);
}
