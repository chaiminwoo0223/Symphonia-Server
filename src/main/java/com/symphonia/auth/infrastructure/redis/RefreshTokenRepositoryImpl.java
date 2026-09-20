package com.symphonia.auth.infrastructure.redis;

import com.symphonia.auth.domain.exception.RefreshTokenHashingFailedException;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String CONSUME_LOCK_KEY_PREFIX = "refresh_token:consume:";

    // 재발급 한 건이 처리되는 데 걸리는 시간보다 넉넉히 크게 잡은 값으로, 락 해제를 별도로 구현하지 않고 TTL 만료에 맡긴다.
    private static final Duration CONSUME_LOCK_TTL = Duration.ofSeconds(5);

    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String value, String memberId, Long expirationTime) {
        RefreshToken refreshToken = RefreshToken.of(hash(value), memberId, expirationTime);

        refreshTokenRedisRepository.save(refreshToken);
    }

    @Override
    public Optional<String> findMemberIdByValue(String value) {
        return refreshTokenRedisRepository.findById(hash(value)).map(RefreshToken::getMemberId);
    }

    // 동일 토큰의 동시 재사용을 막기 위해 SET NX로 소비 권한을 선점한 요청만 조회와 삭제를 진행한다.
    // 소비한 토큰 하나만 지우므로 같은 회원의 다른 기기 토큰은 유지된다.
    @Override
    public Optional<String> consume(String value) {
        String hashedValue = hash(value);

        if (!tryAcquireConsumeLock(hashedValue)) {
            return Optional.empty();
        }

        return refreshTokenRedisRepository
                .findById(hashedValue)
                .map(
                        token -> {
                            refreshTokenRedisRepository.deleteById(hashedValue);
                            return token.getMemberId();
                        });
    }

    @Override
    public void delete(String memberId) {
        // deleteByMemberId 파생 쿼리는 @Indexed 필드 기반 삭제를 실제로 수행하지 않아 findAllByMemberId + deleteAll로 대체한다.
        List<RefreshToken> refreshTokens = refreshTokenRedisRepository.findAllByMemberId(memberId);
        refreshTokenRedisRepository.deleteAll(refreshTokens);
    }

    // setIfAbsent는 파이프라인/트랜잭션에서 null을 반환할 수 있어, null도 획득 실패로 취급한다.
    private boolean tryAcquireConsumeLock(String hashedValue) {
        return Boolean.TRUE.equals(
                redisTemplate
                        .opsForValue()
                        .setIfAbsent(CONSUME_LOCK_KEY_PREFIX + hashedValue, "1", CONSUME_LOCK_TTL));
    }

    // Redis에는 멤버십 확인용으로만 쓰이므로 복호화가 필요 없어, 원본 토큰 대신 단방향 해시를 저장한다.
    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashedBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RefreshTokenHashingFailedException();
        }
    }
}
