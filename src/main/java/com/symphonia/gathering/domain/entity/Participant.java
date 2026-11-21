package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.vo.PhoneNumber;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Participant {
    private Long id;
    private String name;
    private PhoneNumber phoneNumber;
    private String consentVersion;
    private LocalDateTime consentedAt;

    // 새 참여자는 Gathering.join으로 만든다. 이름은 앞뒤 공백을 제거해 저장한다.
    static Participant of(
            String name,
            PhoneNumber phoneNumber,
            String consentVersion,
            LocalDateTime consentedAt) {
        return Participant.builder()
                .name(name.strip())
                .phoneNumber(phoneNumber)
                .consentVersion(consentVersion)
                .consentedAt(consentedAt)
                .build();
    }
}
