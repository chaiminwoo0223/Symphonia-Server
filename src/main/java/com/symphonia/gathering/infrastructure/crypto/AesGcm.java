package com.symphonia.gathering.infrastructure.crypto;

import com.symphonia.gathering.domain.exception.GatheringEncryptionFailedException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// 암호문은 매번 새로 뽑은 IV 12바이트 뒤에 GCM 암호문과 인증 태그를 붙인 형태로 저장한다.
final class AesGcm {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int MIN_CIPHERTEXT_LENGTH_BYTES = IV_LENGTH_BYTES + TAG_LENGTH_BITS / 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AesGcm() {}

    public static byte[] encrypt(SecretKey key, byte[] plaintext) {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext);

            return ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
        } catch (GeneralSecurityException e) {
            throw new GatheringEncryptionFailedException();
        }
    }

    public static byte[] decrypt(SecretKey key, byte[] ciphertext) {
        validateCiphertextLength(ciphertext);
        byte[] iv = Arrays.copyOfRange(ciphertext, 0, IV_LENGTH_BYTES);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            return cipher.doFinal(ciphertext, IV_LENGTH_BYTES, ciphertext.length - IV_LENGTH_BYTES);
        } catch (GeneralSecurityException e) {
            throw new GatheringEncryptionFailedException();
        }
    }

    // IV와 인증 태그보다 짧은 암호문은 doFinal이 GeneralSecurityException이 아닌 예외를 던져서 먼저 걸러 낸다.
    private static void validateCiphertextLength(byte[] ciphertext) {
        if (ciphertext.length < MIN_CIPHERTEXT_LENGTH_BYTES) {
            throw new GatheringEncryptionFailedException();
        }
    }
}
