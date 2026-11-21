package com.symphonia.gathering.application.service;

import com.symphonia.auth.application.usecase.VerifySocialTokenUseCase;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.common.exception.BusinessException;
import com.symphonia.gathering.application.dto.command.JoinGatheringAsHostCommand;
import com.symphonia.gathering.application.dto.command.JoinGatheringCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.usecase.JoinGatheringAsHostUseCase;
import com.symphonia.gathering.application.usecase.JoinGatheringUseCase;
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
public class JoinGatheringService implements JoinGatheringUseCase, JoinGatheringAsHostUseCase {
    // auth의 SocialClient 구현체 이름이다. 참여자의 본인 확인에는 구글만 쓴다.
    private static final String GOOGLE_PROVIDER = "google";

    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final ParticipantGoogleSubjectRepository participantGoogleSubjectRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final GatheringTokenProvider gatheringTokenProvider;
    private final VerifySocialTokenUseCase verifySocialTokenUseCase;
    private final Clock clock;

    // 구글 ID 토큰은 본인 확인에만 쓰고 sub 외의 값은 읽지 않는다. 외부 호출이라 모임 행을 잠그기 전에 검증한다.
    @Override
    public ParticipantResult join(JoinGatheringCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Long gatheringId =
                gatheringJoinTokenRepository
                        .findGatheringIdByValue(command.joinToken())
                        .orElseThrow(JoinTokenNotFoundException::new);
        validateRetained(gatheringId, now, JoinTokenNotFoundException::new);
        String googleSubject =
                verifySocialTokenUseCase
                        .verify(GOOGLE_PROVIDER, command.googleIdToken())
                        .socialId();
        Gathering gathering =
                gatheringRepository
                        .findByIdForUpdate(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);
        boolean joinedByGoogleAccount =
                participantGoogleSubjectRepository.exists(gatheringId, googleSubject);

        ParticipantResult result =
                joinParticipant(
                        gathering,
                        command.name(),
                        command.phoneNumber(),
                        command.consentAgreed(),
                        command.consentVersion(),
                        joinedByGoogleAccount,
                        now);
        participantGoogleSubjectRepository.save(gatheringId, result.id(), googleSubject);

        return result;
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

        return joinParticipant(
                gathering,
                command.name(),
                command.phoneNumber(),
                command.consentAgreed(),
                command.consentVersion(),
                false,
                now);
    }

    // 호출하는 쪽이 모임 행을 잠근 채로 넘겨야 인원 확인과 저장 사이에 다른 참여가 끼어들지 않는다.
    private ParticipantResult joinParticipant(
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
        Participant participant = savedGathering.getParticipant(phoneNumber);
        String linkToken = gatheringTokenProvider.generate();
        participantLinkTokenRepository.save(linkToken, participant.getId());

        return ParticipantResult.of(participant, linkToken);
    }

    // 파기 시각이 지난 모임은 물리 삭제 전이라도 찾지 못한 것으로 본다. 삭제 뒤의 응답과 같게 하기 위해서다.
    private void validateRetained(
            Long gatheringId, LocalDateTime now, Supplier<? extends BusinessException> notFound) {
        gatheringRetentionRepository
                .findByGatheringId(gatheringId)
                .filter(retention -> !retention.isExpired(now))
                .orElseThrow(notFound);
    }
}
