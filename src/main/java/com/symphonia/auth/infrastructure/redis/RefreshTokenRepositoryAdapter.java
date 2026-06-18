package com.symphonia.auth.infrastructure.redis;

import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    @Override
    public void save(String value, String memberId, Long expirationTime) {
        RefreshToken refreshToken = RefreshToken.of(value, memberId, expirationTime);

        refreshTokenRedisRepository.save(refreshToken);
    }

    @Override
    public Optional<String> findMemberIdByValue(String value) {
        return refreshTokenRedisRepository.findById(value)
                .map(RefreshToken::getMemberId);
    }

    @Override
    public void deleteByMemberId(String memberId) {
        refreshTokenRedisRepository.deleteByMemberId(memberId);
    }
}
