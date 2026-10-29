package com.symphonia.gathering.infrastructure.crypto;

import com.symphonia.gathering.domain.exception.GatheringEncryptionFailedException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.NonNull;

// 모임마다 따로 만드는 키 한 쌍이다. 이 키를 지우면 그 모임의 암호문과 휴대폰 HMAC을 더는 풀거나 맞춰 볼 수 없다.
public record GatheringDataKey(SecretKey cipherKey, SecretKey hmacKey) {
    private static final String CIPHER_KEY_ALGORITHM = "AES";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static GatheringDataKey generate() {
        return of(randomBytes(), randomBytes());
    }

    public static GatheringDataKey of(byte[] cipherKey, byte[] hmacKey) {
        return new GatheringDataKey(
                new SecretKeySpec(cipherKey, CIPHER_KEY_ALGORITHM),
                new SecretKeySpec(hmacKey, HMAC_ALGORITHM));
    }

    public byte[] encrypt(String plaintext) {
        return AesGcm.encrypt(cipherKey, plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public String decrypt(byte[] ciphertext) {
        return new String(AesGcm.decrypt(cipherKey, ciphertext), StandardCharsets.UTF_8);
    }

    // 암호문은 IV가 매번 달라 비교할 수 없어, 같은 모임 안의 휴대폰 중복은 이 HMAC으로 확인한다.
    public String hmac(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);

            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new GatheringEncryptionFailedException();
        }
    }

    // 키 원문이 로그에 남지 않게 한다.
    @Override
    public @NonNull String toString() {
        return "GatheringDataKey[]";
    }

    private static byte[] randomBytes() {
        byte[] bytes = new byte[KEY_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(bytes);

        return bytes;
    }
}
