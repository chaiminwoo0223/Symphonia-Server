package com.symphonia.member.application.usecase;

import com.symphonia.member.application.dto.command.CreateMemberCommand;
import com.symphonia.member.application.dto.result.MemberResult;

public interface CreateMemberUseCase {
    MemberResult create(CreateMemberCommand command);
}
