package com.symphonia.gathering.domain.error;

import com.symphonia.common.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum GatheringErrorCode implements ErrorCode {
    // 400
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "휴대폰 번호 형식이 올바르지 않습니다."),
    GATHERING_JOIN_CLOSED(HttpStatus.BAD_REQUEST, "참여 기간이 끝난 모임입니다."),

    // 403
    NOT_GATHERING_HOST(HttpStatus.FORBIDDEN, "모임을 만든 회원만 요청할 수 있습니다."),

    // 404
    GATHERING_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 모임을 찾을 수 없습니다."),

    // 409
    PARTICIPANT_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 이 모임에 참여한 휴대폰 번호입니다."),

    // 500
    INVALID_GATHERING_MASTER_KEY(
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
