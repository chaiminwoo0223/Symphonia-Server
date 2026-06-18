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
    public void save(RefreshToken refreshToken) {
        refreshTokenRedisRepository.save(refreshToken);
    }

    @Override
    public Optional<String> find(String memberId) {
        return refreshTokenRedisRepository.findById(memberId)
                .map(RefreshToken::getValue);
    }

    @Override
    public void delete(String memberId) {
        refreshTokenRedisRepository.deleteById(memberId);
    }
}
