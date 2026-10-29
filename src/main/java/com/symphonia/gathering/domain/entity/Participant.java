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
    private boolean nonDrinker;
    private String consentVersion;
    private LocalDateTime consentedAt;

    // 새 참여자는 Gathering.join으로 만든다.
    static Participant of(
            String name,
            PhoneNumber phoneNumber,
            boolean nonDrinker,
            String consentVersion,
            LocalDateTime consentedAt) {
        return Participant.builder()
                .name(name)
                .phoneNumber(phoneNumber)
                .nonDrinker(nonDrinker)
                .consentVersion(consentVersion)
                .consentedAt(consentedAt)
                .build();
    }
}
