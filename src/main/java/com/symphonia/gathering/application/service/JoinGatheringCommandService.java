package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.common.exception.BusinessException;
import com.symphonia.gathering.application.dto.command.AddParticipantCommand;
import com.symphonia.gathering.application.dto.command.JoinGatheringAsHostCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.usecase.AddParticipantUseCase;
import com.symphonia.gathering.application.usecase.JoinGatheringAsHostUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.JoinTokenNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantGoogleSubjectRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class JoinGatheringCommandService
        implements AddParticipantUseCase, JoinGatheringAsHostUseCase {
    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final ParticipantGoogleSubjectRepository participantGoogleSubjectRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final GatheringTokenProvider gatheringTokenProvider;
    private final Clock clock;

    @Override
    public ParticipantResult add(AddParticipantCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Long gatheringId =
                gatheringJoinTokenRepository
                        .findGatheringIdByValue(command.joinToken())
                        .orElseThrow(JoinTokenNotFoundException::new);
        validateRetained(gatheringId, now, JoinTokenNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findByIdForUpdate(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);
        boolean joinedByGoogleAccount =
                participantGoogleSubjectRepository.exists(gatheringId, command.googleSubject());

        Participant participant =
                addParticipant(
                        gathering,
                        command.name(),
                        command.phoneNumber(),
                        command.consentAgreed(),
                        command.consentVersion(),
                        joinedByGoogleAccount,
                        now);
        participantGoogleSubjectRepository.save(
                gatheringId, participant.getId(), command.googleSubject());

        return issueLinkToken(participant);
    }

    // 모임장도 참여 QR 없이 로그인한 채로 참여자가 될 수 있다. 참여 기간, 동의, 휴대폰 중복, 인원 상한은 QR 참여와 같은 규칙을 따른다.
    // 참여자가 되면 정산 건 인원에 들어갈 수 있고, 그 정산 건의 결제자에게 이름과 휴대폰이 전달되므로 동의를 똑같이 받는다.
    @Override
    public ParticipantResult joinAsHost(JoinGatheringAsHostCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        validateRetained(command.gatheringId(), now, GatheringNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findByIdForUpdate(command.gatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), command.memberId());

        Participant participant =
                addParticipant(
                        gathering,
                        command.name(),
                        command.phoneNumber(),
                        command.consentAgreed(),
                        command.consentVersion(),
                        false,
                        now);

        return issueLinkToken(participant);
    }

    // 파기 시각이 지난 모임은 물리 삭제 전이라도 찾지 못한 것으로 본다. 삭제 뒤의 응답과 같게 하기 위해서다.
    private void validateRetained(
            Long gatheringId, LocalDateTime now, Supplier<? extends BusinessException> notFound) {
        gatheringRetentionRepository
                .findByGatheringId(gatheringId)
                .filter(retention -> !retention.isExpired(now))
                .orElseThrow(notFound);
    }

    // 호출하는 쪽이 모임 행을 잠근 채로 넘겨야 인원 확인과 저장 사이에 다른 참여가 끼어들지 않는다.
    private Participant addParticipant(
            Gathering gathering,
            String name,
            String rawPhoneNumber,
            boolean consentAgreed,
            String consentVersion,
            boolean joinedByGoogleAccount,
            LocalDateTime now) {
        PhoneNumber phoneNumber = PhoneNumber.from(rawPhoneNumber);
        GatheringPolicy.validateJoinable(gathering.getRecordingClosesAt(), now);
        GatheringPolicy.validateConsent(consentAgreed, consentVersion);
        GatheringPolicy.validateNotJoined(
                gathering.hasJoined(phoneNumber) || joinedByGoogleAccount);
        GatheringPolicy.validateParticipantAddable(gathering.getParticipants().size());

        gathering.join(name, phoneNumber, consentVersion, now);
        Gathering savedGathering = gatheringRepository.save(gathering);

        return savedGathering.getParticipant(phoneNumber);
    }

    // 개인 링크 토큰은 Redis에 저장되어 롤백되지 않으므로, 롤백될 수 있는 저장을 모두 마친 뒤에 발급한다.
    private ParticipantResult issueLinkToken(Participant participant) {
        String linkToken = gatheringTokenProvider.generate();
        participantLinkTokenRepository.save(linkToken, participant.getId());

        return ParticipantResult.of(participant, linkToken);
    }
}
