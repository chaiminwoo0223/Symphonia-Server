package com.symphonia.member.application.usecase;

import com.symphonia.member.application.dto.command.DeleteMemberCommand;

public interface DeleteMemberUseCase {
    void delete(DeleteMemberCommand command);
}
