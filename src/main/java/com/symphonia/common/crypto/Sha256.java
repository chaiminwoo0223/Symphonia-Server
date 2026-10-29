package com.symphonia.common.crypto;

import com.symphonia.common.exception.InternalServerException;
import com.symphonia.common.exception.error.CommonErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// 원본을 되돌릴 필요 없이 같은 값인지만 확인하는 토큰을 단방향 해시로 저장할 때 쓴다.
// 솔트가 없으므로 무작위 토큰처럼 추측할 수 없는 값에만 쓴다.
public final class Sha256 {
    private static final String ALGORITHM = "SHA-256";

    private Sha256() {}

    // 모든 JDK는 SHA-256을 지원해야 하므로 NoSuchAlgorithmException은 실제로 발생하지 않는다.
    public static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashedBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalServerException(CommonErrorCode.HASHING_FAILED);
        }
    }
}
