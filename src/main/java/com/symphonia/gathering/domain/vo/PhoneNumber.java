package com.symphonia.gathering.domain.vo;

import com.symphonia.gathering.domain.exception.InvalidPhoneNumberException;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

public record PhoneNumber(String value) {
    // 저장된 번호를 복원할 때 쓰는 규칙이다. 예전에 받은 011 같은 번호도 정산 화면에서 복원할 수 있어야 한다.
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^01[016789]\\d{7,8}$");

    // 새로 받는 입력의 규칙이다. 010으로 시작하는 11자리만 받는다.
    private static final Pattern INPUT_PATTERN = Pattern.compile("^010\\d{8}$");
    private static final int VISIBLE_PREFIX_LENGTH = 3;
    private static final int VISIBLE_SUFFIX_LENGTH = 4;

    public PhoneNumber {
        if (value == null || !MOBILE_PATTERN.matcher(value).matches()) {
            throw new InvalidPhoneNumberException();
        }
    }

    // 입력은 하이픈을 섞어 받을 수 있어 하이픈을 지워 정규화한다. +82나 공백이 섞인 번호의 형식 정리는 클라이언트가 맡는다.
    public static PhoneNumber from(String input) {
        String normalized = input == null ? null : input.replace("-", "");
        if (normalized == null || !INPUT_PATTERN.matcher(normalized).matches()) {
            throw new InvalidPhoneNumberException();
        }

        return new PhoneNumber(normalized);
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
