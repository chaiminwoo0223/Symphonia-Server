package com.symphonia.gathering.domain.vo;

import com.symphonia.gathering.domain.exception.InvalidPhoneNumberException;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

public record PhoneNumber(String value) {
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^01[016789]\\d{7,8}$");
    private static final int VISIBLE_PREFIX_LENGTH = 3;
    private static final int VISIBLE_SUFFIX_LENGTH = 4;

    public PhoneNumber {
        if (value == null || !MOBILE_PATTERN.matcher(value).matches()) {
            throw new InvalidPhoneNumberException();
        }
    }

    // 입력은 하이픈을 섞어 받을 수 있어 하이픈을 지워 정규화한다.
    public static PhoneNumber from(String input) {
        return new PhoneNumber(input == null ? null : input.replace("-", ""));
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
