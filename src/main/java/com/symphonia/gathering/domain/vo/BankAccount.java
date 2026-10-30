package com.symphonia.gathering.domain.vo;

import com.symphonia.gathering.domain.exception.InvalidBankAccountException;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

public record BankAccount(String bankName, String accountNumber) {
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^\\d{10,16}$");
    private static final int VISIBLE_SUFFIX_LENGTH = 4;

    public BankAccount {
        if (bankName == null
                || bankName.isBlank()
                || accountNumber == null
                || !ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches()) {
            throw new InvalidBankAccountException();
        }
    }

    // 계좌번호는 하이픈을 섞어 받을 수 있어 하이픈을 지워 정규화한다.
    public static BankAccount of(String bankName, String accountNumber) {
        return new BankAccount(
                bankName == null ? null : bankName.strip(),
                accountNumber == null ? null : accountNumber.replace("-", ""));
    }

    // 로그나 예외 메시지에 계좌번호가 남지 않게 끝 네 자리만 내보낸다.
    @Override
    public @NonNull String toString() {
        return bankName
                + " ****"
                + accountNumber.substring(accountNumber.length() - VISIBLE_SUFFIX_LENGTH);
    }
}
