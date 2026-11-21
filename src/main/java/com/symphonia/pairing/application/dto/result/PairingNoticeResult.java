package com.symphonia.pairing.application.dto.result;

import com.symphonia.pairing.domain.vo.PairingNotice;

public record PairingNoticeResult(String code, String message) {
    public static PairingNoticeResult from(PairingNotice notice) {
        return new PairingNoticeResult(notice.name(), notice.message());
    }
}
