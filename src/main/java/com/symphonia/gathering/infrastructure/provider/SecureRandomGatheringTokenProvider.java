package com.symphonia.gathering.infrastructure.provider;

import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class SecureRandomGatheringTokenProvider implements GatheringTokenProvider {
    private static final int TOKEN_LENGTH_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String generate() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(tokenBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
