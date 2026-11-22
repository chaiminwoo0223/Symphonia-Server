package com.symphonia.gathering.application.service;

import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.IntegrationTest;
import com.symphonia.gathering.application.dto.command.CorrectParticipantByGoogleSubjectCommand;
import com.symphonia.gathering.application.usecase.CorrectParticipantByGoogleSubjectUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.exception.ParticipantAlreadyJoinedException;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantGoogleSubjectRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

// 같은 모임의 두 참여자가 같은 새 휴대폰으로 동시에 정정하면 한 명만 바뀌어야 한다(#220).
// 정정은 모임 행을 잠가 휴대폰 중복 검사와 저장을 차례로 처리하고, 늦은 요청은 409로 끝난다.
// 트랜잭션 롤백 없이 커밋하므로 직접 지운다.
@DisplayName("참여자 정정 동시성 통합 테스트")
class ParticipantCorrectionConcurrencyIntegrationTest extends IntegrationTest {

    private static final String KIM_SUBJECT = "109876543210987654321";
    private static final String LEE_SUBJECT = "109876543210987654322";
    private static final String NEW_PHONE_NUMBER = "01077778888";

    @Autowired private CorrectParticipantByGoogleSubjectUseCase correctUseCase;
    @Autowired private ParticipantGoogleSubjectRepository participantGoogleSubjectRepository;
    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private Member host;
    private Gathering gathering;
    private Participant kim;
    private Participant lee;
    private String joinToken;

    // 저장소는 부른 쪽의 트랜잭션을 전제로 하므로, 테스트 트랜잭션 없이 도는 준비도 트랜잭션 안에서 저장한다.
    @BeforeEach
    void saveGathering() {
        host = memberHelper.save(MemberFixture.KAKAO);
        transactionTemplate.executeWithoutResult(
                _ -> {
                    gathering =
                            gatheringHelper.join(
                                    gatheringHelper.save(
                                            GatheringFixture.FRIDAY_DINNER, host.getId()),
                                    ParticipantFixture.KIM,
                                    ParticipantFixture.LEE);
                    kim = gathering.getParticipants().get(0);
                    lee = gathering.getParticipants().get(1);
                    participantGoogleSubjectRepository.save(
                            gathering.getId(), kim.getId(), KIM_SUBJECT);
                    participantGoogleSubjectRepository.save(
                            gathering.getId(), lee.getId(), LEE_SUBJECT);
                    joinToken = gatheringHelper.issueJoinToken(gathering);
                });
    }

    @AfterEach
    void deleteCommittedRows() {
        gatheringHelper.deleteRows(gathering.getId());
        memberRepository.delete(host);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("같은 새 휴대폰으로 겹쳐 정정해도 한 요청만 성공하고 늦은 요청은 ParticipantAlreadyJoinedException이다")
    void shouldLetOnlyOneParticipantTakePhoneNumberWhenCorrectedConcurrently() {
        // when
        List<Throwable> failures =
                runConcurrently(
                        () -> correctUseCase.correctByGoogleSubject(command(KIM_SUBJECT, "김영수")),
                        () -> correctUseCase.correctByGoogleSubject(command(LEE_SUBJECT, "이영희")));

        // then
        assertThat(failures).hasSize(1);
        assertThat(failures.getFirst()).isInstanceOf(ParticipantAlreadyJoinedException.class);
        List<Participant> participants =
                gatheringRepository.findById(gathering.getId()).orElseThrow().getParticipants();
        assertThat(participants)
                .filteredOn(
                        participant ->
                                NEW_PHONE_NUMBER.equals(participant.getPhoneNumber().value()))
                .hasSize(1);
    }

    private CorrectParticipantByGoogleSubjectCommand command(String googleSubject, String name) {
        return new CorrectParticipantByGoogleSubjectCommand(
                joinToken, googleSubject, name, NEW_PHONE_NUMBER);
    }
}
