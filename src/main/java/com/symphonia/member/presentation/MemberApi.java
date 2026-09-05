package com.symphonia.member.presentation;

import com.symphonia.global.common.response.StandardResponse;
import com.symphonia.member.presentation.dto.request.MemberUpdateRequest;
import com.symphonia.member.presentation.dto.response.MemberResponse;
import com.symphonia.member.presentation.dto.response.MemberUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/members")
@Tag(name = "Member API", description = "멤버 API")
public interface MemberApi {

    @GetMapping("/me")
    @Operation(summary = "멤버 조회", description = "인증된 멤버의 정보를 조회합니다.")
    ResponseEntity<StandardResponse<MemberResponse>> get(@AuthenticationPrincipal String memberId);

    @PatchMapping("/me")
    @Operation(summary = "멤버 수정", description = "인증된 멤버의 정보를 수정합니다.")
    ResponseEntity<StandardResponse<MemberUpdateResponse>> update(
            @AuthenticationPrincipal String memberId, @RequestBody MemberUpdateRequest request);

    @DeleteMapping("/me")
    @Operation(summary = "멤버 삭제", description = "인증된 멤버를 삭제합니다.")
    ResponseEntity<StandardResponse<Void>> delete(@AuthenticationPrincipal String memberId);
}
