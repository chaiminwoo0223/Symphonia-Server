package com.symphonia.auth.infrastructure.redis;

import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    @Override
    public void save(String value, String memberId, Long expirationTime) {
        RefreshToken refreshToken = RefreshToken.of(value, memberId, expirationTime);

        refreshTokenRedisRepository.save(refreshToken);
    }

    @Override
    public Optional<String> findMemberIdByValue(String value) {
        return refreshTokenRedisRepository.findById(value).map(RefreshToken::getMemberId);
    }

    @Override
    public void delete(String memberId) {
        // deleteByMemberId 파생 쿼리는 @Indexed 필드 기반 삭제를 실제로 수행하지 않아
        // findAllByMemberId + deleteAll로 대체한다(2026-09-05, RefreshTokenRepositoryImplTest에서 발견).
        refreshTokenRedisRepository.deleteAll(
                refreshTokenRedisRepository.findAllByMemberId(memberId));
    }
}
