package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.common.exception.BusinessException;
import com.symphonia.gathering.application.dto.command.CorrectParticipantByGoogleSubjectCommand;
import com.symphonia.gathering.application.dto.command.ReissueParticipantLinkTokenByGoogleSubjectCommand;
import com.symphonia.gathering.application.dto.command.ReissueParticipantLinkTokenCommand;
import com.symphonia.gathering.application.dto.command.RemoveParticipantBySubjectCommand;
import com.symphonia.gathering.application.dto.command.RemoveParticipantCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.usecase.CorrectParticipantByGoogleSubjectUseCase;
import com.symphonia.gathering.application.usecase.ReissueParticipantLinkTokenByGoogleSubjectUseCase;
import com.symphonia.gathering.application.usecase.ReissueParticipantLinkTokenUseCase;
import com.symphonia.gathering.application.usecase.RemoveParticipantBySubjectUseCase;
import com.symphonia.gathering.application.usecase.RemoveParticipantUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.JoinTokenNotFoundException;
import com.symphonia.gathering.domain.exception.ParticipantNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantGoogleSubjectRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.ParticipantPairingFeedbackRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.PhoneNumber;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

// 모임장이 참여자를 정리하거나 링크를 잃은 참여자를 돕는 흐름이고, 참여자가 스스로 링크를 복구하거나 철회하거나 정정하는 흐름도 함께 맡는다.
// 링크를 잃은 참여자도 파기 전까지 분담금을 보내야 하므로 기록 기간 제한을 두지 않는다.
@CommandService
@RequiredArgsConstructor
public class ParticipantCommandService
        implements RemoveParticipantUseCase,
                RemoveParticipantBySubjectUseCase,
                ReissueParticipantLinkTokenUseCase,
                ReissueParticipantLinkTokenByGoogleSubjectUseCase,
                CorrectParticipantByGoogleSubjectUseCase {
    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;
    private final ParticipantGoogleSubjectRepository participantGoogleSubjectRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final ParticipantPairingFeedbackRepository participantPairingFeedbackRepository;
    private final SettlementRepository settlementRepository;
    private final GatheringTokenProvider gatheringTokenProvider;
    private final Clock clock;

    // 개인 링크 토큰은 참여자마다 여러 개를 둘 수 있으므로, 새로 발급하기 전에 이전 토큰을 모두 지워 이전 링크를 무효로 만든다.
    // 참여자에게는 토큰이 하나만 남아야 하는데 DB가 막지 않으므로, 모임 행을 잠가 같은 모임의 재발급을 차례로 처리한다.
    @Override
    public ParticipantResult reissue(ReissueParticipantLinkTokenCommand command) {
        validateRetained(command.gatheringId(), GatheringNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findByIdForUpdate(command.gatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), command.memberId());
        GatheringPolicy.validateParticipantExists(
                gathering.hasParticipant(command.participantId()));

        return reissueLinkToken(gathering.getParticipant(command.participantId()));
    }

    // 본인 확인을 마친 구글 계정으로 참여한 참여자에게만 새 링크를 준다. 모임은 참여 토큰으로 찾는다.
    // 파기 시각이 지난 모임은 참여 때와 같이 참여 토큰을 찾지 못한 것으로 본다.
    // 모임장의 재발급과 같은 이유로 모임 행을 잠가 같은 모임의 재발급을 차례로 처리한다.
    @Override
    public ParticipantResult reissueByGoogleSubject(
            ReissueParticipantLinkTokenByGoogleSubjectCommand command) {
        Long gatheringId =
                gatheringJoinTokenRepository
                        .findGatheringIdByValue(command.joinToken())
                        .orElseThrow(JoinTokenNotFoundException::new);
        validateRetained(gatheringId, JoinTokenNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findByIdForUpdate(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);
        Long participantId =
                participantGoogleSubjectRepository
                        .findParticipantId(gatheringId, command.googleSubject())
                        .orElseThrow(ParticipantNotFoundException::new);

        return reissueLinkToken(gathering.getParticipant(participantId));
    }

    // 본인 확인을 마친 구글 계정으로 참여한 참여자의 이름과 휴대폰을 정정한다. 모임은 참여 토큰으로 찾는다.
    // 파기 시각이 지난 모임은 참여 때와 같이 참여 토큰을 찾지 못한 것으로 보고, 기록 기간 제한은 두지 않는다.
    // 모임 행을 잠가 같은 모임의 참여와 정정이 겹쳐도 휴대폰 중복 검사와 저장이 차례로 처리되게 한다.
    // 정산 건에 든 참여자도 정정할 수 있다. 정산 화면이 이름과 휴대폰을 매번 읽으므로 금액과 분담금 상태는 바뀌지 않는다.
    @Override
    public void correctByGoogleSubject(CorrectParticipantByGoogleSubjectCommand command) {
        Long gatheringId =
                gatheringJoinTokenRepository
                        .findGatheringIdByValue(command.joinToken())
                        .orElseThrow(JoinTokenNotFoundException::new);
        validateRetained(gatheringId, JoinTokenNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findByIdForUpdate(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);
        Long participantId =
                participantGoogleSubjectRepository
                        .findParticipantId(gatheringId, command.googleSubject())
                        .orElseThrow(ParticipantNotFoundException::new);

        gathering.correctParticipant(
                participantId, command.name(), PhoneNumber.from(command.phoneNumber()));
        gatheringRepository.updateParticipant(gatheringId, gathering.getParticipant(participantId));
    }

    // 모임 행을 잠가 정산 건 기록과 차례로 처리한다. 잠그지 않으면 제거와 동시에 기록된 정산 건이 익명화한 참여자를 가리킬 수 있다.
    @Override
    public void remove(RemoveParticipantCommand command) {
        validateRetained(command.gatheringId(), GatheringNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findByIdForUpdate(command.gatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), command.memberId());
        GatheringPolicy.validateParticipantExists(
                gathering.hasParticipant(command.participantId()));

        removeParticipant(command.participantId());
    }

    // 참여자는 참여할 때 쓴 참여 토큰으로 모임을 찾고, 구글 계정으로 본인을 확인한다. 개인 링크가 없어도 철회할 수 있다.
    // 같은 구글 계정으로 참여한 참여자가 없으면 모임에 없는 참여자와 같게 404(PARTICIPANT_NOT_FOUND)다.
    @Override
    public void remove(RemoveParticipantBySubjectCommand command) {
        Long gatheringId =
                gatheringJoinTokenRepository
                        .findGatheringIdByValue(command.joinToken())
                        .orElseThrow(JoinTokenNotFoundException::new);
        validateRetained(gatheringId, JoinTokenNotFoundException::new);
        // 참여자를 읽을 필요가 없으므로 모임장의 회원 id만 읽는 잠금 조회로 모임 행만 잠근다.
        gatheringRepository
                .findHostMemberIdByIdForUpdate(gatheringId)
                .orElseThrow(GatheringNotFoundException::new);
        Long participantId =
                participantGoogleSubjectRepository
                        .findParticipantId(gatheringId, command.googleSubject())
                        .orElseThrow(ParticipantNotFoundException::new);

        removeParticipant(participantId);
    }

    // 파기 시각이 지난 모임은 물리 삭제 전이라도 찾지 못한 것으로 본다. 삭제 뒤의 응답과 같게 하기 위해서다.
    private void validateRetained(
            Long gatheringId, Supplier<? extends BusinessException> notFound) {
        LocalDateTime now = LocalDateTime.now(clock);
        gatheringRetentionRepository
                .findByGatheringId(gatheringId)
                .filter(retention -> !retention.isExpired(now))
                .orElseThrow(notFound);
    }

    // 호출하는 쪽이 모임 행을 잠근 채로 넘겨야 같은 모임의 재발급이 차례로 처리된다.
    private ParticipantResult reissueLinkToken(Participant participant) {
        String linkToken = gatheringTokenProvider.generate();
        participantLinkTokenRepository.deleteAllByParticipantId(participant.getId());
        participantLinkTokenRepository.save(linkToken, participant.getId());

        return ParticipantResult.of(participant, linkToken);
    }

    // 피드백 제출 표시는 분담금을 보내거나 받은 참여자만 남기지만, 정산 건을 고쳐 참여자를 뺀 뒤에는 표시만 남을 수 있어 함께 지운다.
    // 익명 피드백은 참여자와 이어져 있지 않으므로 지우지 않는다.
    // 취소한 정산 건이 참여자를 가리키면 행을 지울 수 없으므로 개인정보만 비운다. 호출하는 쪽이 모임 행을 잠근 채로 넘겨야 한다.
    private void removeParticipant(Long participantId) {
        GatheringPolicy.validateParticipantRemovable(
                settlementRepository.existsUncancelledByParticipantId(participantId));

        participantLinkTokenRepository.deleteAllByParticipantId(participantId);
        participantPairingFeedbackRepository.deleteByParticipantId(participantId);
        if (settlementRepository.existsByParticipantId(participantId)) {
            gatheringRepository.anonymizeParticipant(participantId, LocalDateTime.now(clock));
        } else {
            gatheringRepository.deleteParticipant(participantId);
        }
    }
}
