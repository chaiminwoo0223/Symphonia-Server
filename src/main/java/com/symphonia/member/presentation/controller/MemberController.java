package com.symphonia.member.presentation.controller;

import com.symphonia.common.response.StandardResponse;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.service.MemberCommandService;
import com.symphonia.member.application.service.MemberQueryService;
import com.symphonia.member.presentation.MemberApi;
import com.symphonia.member.presentation.dto.request.MemberUpdateRequest;
import com.symphonia.member.presentation.dto.response.MemberResponse;
import com.symphonia.member.presentation.dto.response.MemberUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController implements MemberApi {
    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;

    @Override
    public ResponseEntity<StandardResponse<MemberResponse>> get(String memberId) {
        MemberResult result = memberQueryService.getById(Long.parseLong(memberId));
        MemberResponse response = MemberResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<MemberUpdateResponse>> update(
            String memberId, MemberUpdateRequest request) {
        MemberResult result =
                memberCommandService.update(Long.parseLong(memberId), request.toCommand());
        MemberUpdateResponse response = MemberUpdateResponse.from(result);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StandardResponse.success(HttpStatus.OK, response));
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> delete(String memberId) {
        memberCommandService.delete(Long.parseLong(memberId));

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(StandardResponse.success(HttpStatus.NO_CONTENT));
    }
}
