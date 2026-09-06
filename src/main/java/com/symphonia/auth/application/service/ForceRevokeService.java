package com.symphonia.auth.application.service;

import com.symphonia.auth.application.usecase.ForceRevokeUseCase;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.common.annotation.CommandService;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class ForceRevokeService implements ForceRevokeUseCase {
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void forceRevoke(String memberId) {
        refreshTokenRepository.delete(memberId);
    }
}
