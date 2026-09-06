package com.symphonia.member.application.usecase;

import com.symphonia.member.application.dto.result.MemberResult;

public interface GetMemberUseCase {
    MemberResult getById(Long memberId);
}
