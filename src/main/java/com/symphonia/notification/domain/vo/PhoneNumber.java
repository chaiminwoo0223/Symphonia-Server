package com.symphonia.notification.domain.vo;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

// 메시지를 받을 휴대폰 번호다. 형식 검증은 번호를 입력받는 gathering이 하고, notification은 받은 번호를 가려 남기는 일만 한다.
public record PhoneNumber(String value) {
    private static final int VISIBLE_PREFIX_LENGTH = 3;
    private static final int VISIBLE_SUFFIX_LENGTH = 4;

    public PhoneNumber {
        Objects.requireNonNull(value);
    }

    public String masked() {
        int maskedLength = value.length() - VISIBLE_PREFIX_LENGTH - VISIBLE_SUFFIX_LENGTH;

        return value.substring(0, VISIBLE_PREFIX_LENGTH)
                + "-"
                + "*".repeat(maskedLength)
                + "-"
                + value.substring(value.length() - VISIBLE_SUFFIX_LENGTH);
    }

    // 로그나 예외 메시지에 원래 번호가 남지 않게 마스킹한 값만 문자열로 내보낸다.
    @Override
    public @NonNull String toString() {
        return masked();
    }
}
