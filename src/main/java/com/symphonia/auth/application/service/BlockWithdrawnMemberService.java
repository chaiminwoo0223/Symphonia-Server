package com.symphonia.auth.application.service;

import com.symphonia.auth.application.usecase.BlockWithdrawnMemberUseCase;
import com.symphonia.auth.domain.provider.AccessTokenProvider;
import com.symphonia.auth.domain.repository.WithdrawnMemberRepository;
import com.symphonia.common.annotation.CommandService;
import lombok.RequiredArgsConstructor;

// 탈퇴 전에 발급된 액세스 토큰은 탈퇴한 기기 밖에서도 만료까지 유효하다.
// 그래서 탈퇴한 회원 id를 액세스 토큰 유효시간만큼 남기고, 필터가 이 id의 토큰을 인증하지 않게 한다.
@CommandService
@RequiredArgsConstructor
public class BlockWithdrawnMemberService implements BlockWithdrawnMemberUseCase {
    private final AccessTokenProvider accessTokenProvider;
    private final WithdrawnMemberRepository withdrawnMemberRepository;

    @Override
    public void block(String memberId) {
        withdrawnMemberRepository.save(memberId, accessTokenProvider.getExpirationTime());
    }
}
