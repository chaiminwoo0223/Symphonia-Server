package com.symphonia.member.application.usecase;

import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.domain.entity.SocialProvider;

public interface GetMemberUseCase {
    MemberResult getById(Long memberId);

    MemberResult getBySocialLogin(SocialProvider socialProvider, String socialId);
}
