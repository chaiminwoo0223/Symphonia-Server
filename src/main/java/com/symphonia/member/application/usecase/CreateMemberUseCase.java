package com.symphonia.member.application.usecase;

import com.symphonia.member.application.dto.command.MemberCreateCommand;
import com.symphonia.member.application.dto.result.MemberResult;

public interface CreateMemberUseCase {
    MemberResult create(MemberCreateCommand command);
}
