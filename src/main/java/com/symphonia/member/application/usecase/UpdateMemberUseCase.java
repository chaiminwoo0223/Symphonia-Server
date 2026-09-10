package com.symphonia.member.application.usecase;

import com.symphonia.member.application.dto.command.MemberUpdateCommand;
import com.symphonia.member.application.dto.result.MemberResult;

public interface UpdateMemberUseCase {
    MemberResult update(Long memberId, MemberUpdateCommand command);
}
