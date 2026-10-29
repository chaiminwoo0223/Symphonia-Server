package com.symphonia.gathering.infrastructure.crypto;

import com.symphonia.gathering.domain.exception.InvalidGatheringMasterKeyException;
import com.symphonia.gathering.infrastructure.crypto.config.properties.GatheringCryptoProperties;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

// 모임별 데이터 키를 감싸 저장하고 다시 푸는 마스터 키다. 마스터 키 자체는 환경변수로만 주입한다.
@Component
public class GatheringMasterKey {
    private static final String ALGORITHM = "AES";
    private static final int KEY_LENGTH_BYTES = 32;

    private final SecretKey masterKey;

    public GatheringMasterKey(GatheringCryptoProperties gatheringCryptoProperties) {
        this.masterKey = decode(gatheringCryptoProperties.masterKey());
    }

    public WrappedDataKey wrap(GatheringDataKey dataKey) {
        return new WrappedDataKey(
                AesGcm.encrypt(masterKey, dataKey.cipherKey().getEncoded()),
                AesGcm.encrypt(masterKey, dataKey.hmacKey().getEncoded()));
    }

    public GatheringDataKey unwrap(WrappedDataKey wrappedDataKey) {
        return GatheringDataKey.of(
                AesGcm.decrypt(masterKey, wrappedDataKey.encryptedCipherKey()),
                AesGcm.decrypt(masterKey, wrappedDataKey.encryptedHmacKey()));
    }

    public record WrappedDataKey(byte[] encryptedCipherKey, byte[] encryptedHmacKey) {}

    // 잘못된 마스터 키로 기동하면 첫 참여 요청에서야 실패하므로, 빈을 만들 때 길이까지 확인한다.
    private static SecretKey decode(String encodedMasterKey) {
        byte[] keyBytes = decodeBase64(encodedMasterKey);
        validateKeyLength(keyBytes);

        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    private static byte[] decodeBase64(String encodedMasterKey) {
        try {
            return Base64.getDecoder().decode(encodedMasterKey);
        } catch (IllegalArgumentException e) {
            throw new InvalidGatheringMasterKeyException();
        }
    }

    private static void validateKeyLength(byte[] keyBytes) {
        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new InvalidGatheringMasterKeyException();
        }
    }
}
