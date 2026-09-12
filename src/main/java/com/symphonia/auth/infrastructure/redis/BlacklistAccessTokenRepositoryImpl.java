package com.symphonia.auth.infrastructure.redis;

import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BlacklistAccessTokenRepositoryImpl implements BlacklistAccessTokenRepository {
    private static final String HASH_ALGORITHM = "SHA-256";

    private final BlacklistAccessTokenRedisRepository blacklistAccessTokenRedisRepository;

    @Override
    public void save(String accessToken, String memberId, Long expirationTime) {
        BlacklistAccessToken blacklistAccessToken =
                BlacklistAccessToken.of(hash(accessToken), memberId, expirationTime);

        blacklistAccessTokenRedisRepository.save(blacklistAccessToken);
    }

    @Override
    public boolean isBlacklisted(String accessToken) {
        return blacklistAccessTokenRedisRepository.existsById(hash(accessToken));
    }

    // Redis에는 멤버십 확인용으로만 쓰이므로 복호화가 필요 없어, 원본 토큰 대신 단방향 해시를 저장한다.
    private String hash(String accessToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashedBytes = digest.digest(accessToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HASH_ALGORITHM + " algorithm not available", e);
        }
    }
}
