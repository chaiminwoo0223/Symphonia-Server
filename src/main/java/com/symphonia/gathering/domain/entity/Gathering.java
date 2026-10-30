package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.vo.PhoneNumber;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Gathering {
    private Long id;
    private Long hostMemberId;
    private String name;
    private LocalDateTime createdAt;
    private List<Participant> participants;

    public static Gathering open(Long hostMemberId, String name) {
        return Gathering.builder()
                .hostMemberId(hostMemberId)
                .name(name)
                .participants(List.of())
                .build();
    }

    // 참여자 목록을 바깥에서 바꿀 수 없게, 참여할 때마다 기존 목록에 더한 새 불변 목록으로 바꾼다.
    public void join(
            String name, PhoneNumber phoneNumber, String consentVersion, LocalDateTime now) {
        Participant participant = Participant.of(name, phoneNumber, consentVersion, now);
        participants = Stream.concat(participants.stream(), Stream.of(participant)).toList();
    }

    public boolean hasJoined(PhoneNumber phoneNumber) {
        return participants.stream()
                .anyMatch(participant -> participant.getPhoneNumber().equals(phoneNumber));
    }

    public boolean hasParticipants(Collection<Long> participantIds) {
        return participants.stream()
                .map(Participant::getId)
                .collect(Collectors.toSet())
                .containsAll(participantIds);
    }

    // 휴대폰은 모임 안에서 중복되지 않으므로, 저장 뒤 id가 채워진 참여자를 휴대폰으로 찾는다.
    // 방금 참여시킨 참여자만 찾으므로, 찾지 못하면 클라이언트 요청이 아니라 서버 버그라서 500으로 둔다.
    public Participant getParticipant(PhoneNumber phoneNumber) {
        return participants.stream()
                .filter(participant -> participant.getPhoneNumber().equals(phoneNumber))
                .findFirst()
                .orElseThrow();
    }

    public List<Participant> getParticipants() {
        return List.copyOf(participants);
    }
}
