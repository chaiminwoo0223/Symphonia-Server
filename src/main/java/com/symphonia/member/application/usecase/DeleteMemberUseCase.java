package com.symphonia.member.application.usecase;

public interface DeleteMemberUseCase {
    void delete(Long memberId, String accessToken);
}
