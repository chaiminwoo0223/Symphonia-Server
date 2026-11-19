package com.symphonia.gathering.domain.entity;

import com.symphonia.gathering.domain.vo.PhoneNumber;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    // 48시간 기록 기간이 시작되는 시각이다. 감사용 생성 시각과 달리 Clock의 시각으로 정한다.
    private LocalDateTime openedAt;
    private List<Participant> participants;
    // 번호 순서로 정렬된 차수다. 모임을 열 때 1차가 생기므로 비어 있지 않다.
    private List<GatheringRound> rounds;

    public static Gathering open(Long hostMemberId, String name, LocalDateTime now) {
        return Gathering.builder()
                .hostMemberId(hostMemberId)
                .name(name)
                .openedAt(now)
                .participants(List.of())
                .rounds(List.of(GatheringRound.first()))
                .build();
    }

    // 참여자 목록을 바깥에서 바꿀 수 없게, 참여할 때마다 기존 목록에 더한 새 불변 목록으로 바꾼다.
    public void join(
            String name, PhoneNumber phoneNumber, String consentVersion, LocalDateTime now) {
        Participant participant = Participant.of(name, phoneNumber, consentVersion, now);
        participants = Stream.concat(participants.stream(), Stream.of(participant)).toList();
    }

    // 차수 목록을 바깥에서 바꿀 수 없게, 추가할 때마다 기존 목록에 더한 새 불변 목록으로 바꾼다.
    public void addRound(int number) {
        rounds = Stream.concat(rounds.stream(), Stream.of(GatheringRound.of(number))).toList();
    }

    public boolean hasRound(Long roundId) {
        return rounds.stream().anyMatch(round -> roundId.equals(round.getId()));
    }

    public Optional<GatheringRound> findRound(int number) {
        return rounds.stream().filter(round -> round.getNumber() == number).findFirst();
    }

    public int getLastRoundNumber() {
        return rounds.getLast().getNumber();
    }

    public boolean hasJoined(PhoneNumber phoneNumber) {
        return participants.stream()
                .anyMatch(participant -> participant.getPhoneNumber().equals(phoneNumber));
    }

    public boolean hasParticipant(Long participantId) {
        return participants.stream()
                .anyMatch(participant -> participantId.equals(participant.getId()));
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

    // 정산 건의 결제자처럼 이 모임에 속한 것이 확인된 참여자만 찾으므로, 찾지 못하면 서버 버그라서 500으로 둔다.
    public Participant getParticipant(Long participantId) {
        return participants.stream()
                .filter(participant -> participant.getId().equals(participantId))
                .findFirst()
                .orElseThrow();
    }

    // 정산 건이 가리키는 차수처럼 이 모임에 속한 것이 확인된 차수만 찾으므로, 찾지 못하면 서버 버그라서 500으로 둔다.
    public GatheringRound getRound(Long roundId) {
        return rounds.stream()
                .filter(round -> roundId.equals(round.getId()))
                .findFirst()
                .orElseThrow();
    }

    // 참여한 순서대로 이름을 돌려준다. 이 모임의 참여자가 아닌 id는 건너뛴다.
    public List<String> getParticipantNames(Set<Long> participantIds) {
        return participants.stream()
                .filter(participant -> participantIds.contains(participant.getId()))
                .map(Participant::getName)
                .toList();
    }

    public List<Participant> getParticipants() {
        return List.copyOf(participants);
    }

    public List<GatheringRound> getRounds() {
        return List.copyOf(rounds);
    }
}
