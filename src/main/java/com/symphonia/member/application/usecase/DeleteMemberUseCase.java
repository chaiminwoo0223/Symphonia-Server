package com.symphonia.member.application.usecase;

import com.symphonia.member.application.dto.command.MemberDeleteCommand;

public interface DeleteMemberUseCase {
    void delete(MemberDeleteCommand command);
}
