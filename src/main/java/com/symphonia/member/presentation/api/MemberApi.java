package com.symphonia.member.presentation.api;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.member.presentation.MemberEndpoints;
import com.symphonia.member.presentation.dto.request.UpdateMemberRequest;
import com.symphonia.member.presentation.dto.response.MemberResponse;
import com.symphonia.member.presentation.dto.response.UpdateMemberResponse;
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

    @DeleteMapping(MemberEndpoints.ME)
    @Operation(summary = "멤버 삭제", description = "인증된 멤버를 삭제합니다.")
    ResponseEntity<StandardResponse<Void>> delete(
            @AuthenticationPrincipal String memberId,
            Authentication authentication,
            HttpServletRequest httpRequest);
}
