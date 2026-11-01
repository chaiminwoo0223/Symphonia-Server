package com.symphonia.gathering.domain.error;

import com.symphonia.common.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum GatheringErrorCode implements ErrorCode {
    // 400
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "휴대폰 번호 형식이 올바르지 않습니다."),
    GATHERING_JOIN_CLOSED(HttpStatus.BAD_REQUEST, "참여 기간이 끝난 모임입니다."),
    CONSENT_REQUIRED(HttpStatus.BAD_REQUEST, "개인정보 수집 및 이용 동의가 필요합니다."),
    OUTDATED_CONSENT_VERSION(HttpStatus.BAD_REQUEST, "개인정보 수집 안내가 바뀌었습니다. 참여 화면을 다시 불러와 주세요."),
    INVALID_BANK_ACCOUNT(HttpStatus.BAD_REQUEST, "계좌번호는 하이픈을 뺀 숫자 10~16자리로 입력해 주세요."),
    BANK_ACCOUNT_CONSENT_REQUIRED(HttpStatus.BAD_REQUEST, "계좌를 정산 건 참여자에게 보여 주는 데 동의가 필요합니다."),
    OUTDATED_BANK_ACCOUNT_CONSENT_VERSION(
            HttpStatus.BAD_REQUEST, "계좌 입력 안내가 바뀌었습니다. 기록 화면을 다시 불러와 주세요."),
    BANK_ACCOUNT_REQUIRED(HttpStatus.BAD_REQUEST, "정산 건을 기록하기 전에 결제자 계좌를 등록해 주세요."),
    SETTLEMENT_RECORDING_CLOSED(HttpStatus.BAD_REQUEST, "정산 건을 기록할 수 있는 기간이 끝난 모임입니다."),
    INVALID_SETTLEMENT_AMOUNT(HttpStatus.BAD_REQUEST, "총액은 0보다 크고, 지정 금액은 0 이상이어야 합니다."),
    FIXED_AMOUNT_EXCEEDS_TOTAL(HttpStatus.BAD_REQUEST, "지정 금액의 합이 총액보다 클 수 없습니다."),
    INVALID_SETTLEMENT_PARTICIPANTS(HttpStatus.BAD_REQUEST, "정산 건 인원이나 지정 금액의 대상이 올바르지 않습니다."),

    // 403
    NOT_GATHERING_HOST(HttpStatus.FORBIDDEN, "모임을 만든 회원만 요청할 수 있습니다."),
    NOT_SETTLEMENT_PAYER(HttpStatus.FORBIDDEN, "정산 건의 결제자만 요청할 수 있습니다."),

    // 404
    GATHERING_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 모임을 찾을 수 없습니다."),
    JOIN_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "유효하지 않은 참여 링크입니다. 모임을 만든 사람에게 새 QR을 받아 주세요."),
    LINK_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "유효하지 않은 링크입니다. 받은 링크를 다시 확인해 주세요."),
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 정산 건을 찾을 수 없습니다."),
    SETTLEMENT_SHARE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 분담금을 찾을 수 없습니다."),

    // 409
    PARTICIPANT_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 이 모임에 참여한 휴대폰 번호입니다."),
    BANK_ACCOUNT_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 계좌를 등록한 참여자입니다."),
    SETTLEMENT_ALREADY_NOTIFIED(
            HttpStatus.CONFLICT, "정산 메시지를 보낸 정산 건은 수정할 수 없습니다. 취소하고 다시 기록해 주세요."),
    SETTLEMENT_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소한 정산 건입니다."),
    SETTLEMENT_CONFLICT(HttpStatus.CONFLICT, "다른 요청이 먼저 정산 건을 바꿨습니다. 다시 불러와 주세요."),
    SETTLEMENT_SHARES_SENT(
            HttpStatus.CONFLICT, "돈을 보낸 참여자가 있는 정산 건은 수정하거나 취소할 수 없습니다. 분담금을 먼저 미입금으로 되돌려 주세요."),
    SHARE_ALREADY_SENT(HttpStatus.CONFLICT, "이미 보냈다고 알린 분담금입니다."),
    SHARE_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확인 완료한 분담금입니다."),
    SHARE_ALREADY_UNPAID(HttpStatus.CONFLICT, "이미 미입금인 분담금입니다."),
    ZERO_SHARE_STATUS_UNCHANGEABLE(
            HttpStatus.CONFLICT, "0원 분담금은 상태를 바꿀 수 없습니다. 금액이 잘못됐으면 정산 건을 수정해 주세요."),

    // 500
    GATHERING_MASTER_KEY_LOAD_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR, "모임 마스터 키는 base64로 인코딩한 256비트 값이어야 합니다."),
    GATHERING_DATA_KEY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "모임의 데이터 키를 찾을 수 없습니다."),
    GATHERING_ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "모임 개인정보의 암호화 처리에 실패했습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }
}
