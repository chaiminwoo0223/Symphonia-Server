package com.symphonia.gathering.application.service;

import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.gathering.application.dto.command.JoinGatheringAsHostCommand;
import com.symphonia.gathering.application.dto.command.JoinGatheringCommand;
import com.symphonia.gathering.application.usecase.JoinGatheringAsHostUseCase;
import com.symphonia.gathering.application.usecase.JoinGatheringUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.exception.GatheringParticipantCountExceedsLimitException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

// 참여 QR은 카카오톡 방에 공유되어 인증 없이 한꺼번에 몰릴 수 있다(#129).
// 모임 행 잠금이 인원 확인과 저장 사이를 직렬화하는지 실제 DB로 확인하므로, 트랜잭션 롤백 없이 커밋하고 직접 지운다.
@DisplayName("모임 참여 동시성 통합 테스트")
class GatheringJoinConcurrencyIntegrationTest extends IntegrationTest {

    private static final int REMAINING_SEATS = 2;
    private static final int CONCURRENT_JOINS = 6;

    @Autowired private JoinGatheringUseCase joinGatheringUseCase;
    @Autowired private JoinGatheringAsHostUseCase joinGatheringAsHostUseCase;
    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @MockitoBean(name = "google")
    private SocialClient googleSocialClient;

    private Member host;
    private Long gatheringId;

    // 요청마다 다른 구글 계정으로 본인을 확인한 것으로 스텁한다. 구글 ID 토큰을 sub로 쓴다.
    @BeforeEach
    void setUpGoogleAccounts() {
        given(googleSocialClient.authenticate(anyString()))
                .willAnswer(
                        invocation ->
                                new SocialIdentity(
                                        invocation.getArgument(0),
                                        "구글 계정",
                                        "symphonia@google.com",
                                        null,
                                        "GOOGLE"));
    }

    @AfterEach
    void deleteCommittedRows() {
        gatheringHelper.deleteRows(gatheringId);
        memberRepository.delete(host);
    }

    @Nested
    @DisplayName("join 메서드는")
    class Join {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("상한 근처에서 동시에 참여해도 상한까지만 참여시키고 나머지는 거절한다")
        void shouldAdmitOnlyUpToLimitWhenJoinedConcurrently() {
            // given
            String joinToken = saveGatheringWithSeats(REMAINING_SEATS);
            List<Runnable> joins =
                    IntStream.range(0, CONCURRENT_JOINS)
                            .<Runnable>mapToObj(
                                    index ->
                                            () ->
                                                    joinGatheringUseCase.join(
                                                            joinCommand(joinToken, index)))
                            .toList();

            // when
            List<Throwable> failures = runConcurrently(joins.toArray(Runnable[]::new));

            // then
            assertThat(failures)
                    .hasSize(CONCURRENT_JOINS - REMAINING_SEATS)
                    .allSatisfy(
                            failure ->
                                    assertThat(failure)
                                            .isInstanceOf(
                                                    GatheringParticipantCountExceedsLimitException
                                                            .class));
            assertThat(participantCount()).isEqualTo(GatheringPolicy.MAX_PARTICIPANT_COUNT);
        }
    }

    @Nested
    @DisplayName("join과 joinAsHost 메서드는")
    class JoinAndJoinAsHost {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("자리가 하나 남았을 때 동시에 요청하면 한 요청만 참여시킨다")
        void shouldAdmitOnlyOneWhenOneSeatLeft() {
            // given
            String joinToken = saveGatheringWithSeats(1);

            // when
            List<Throwable> failures =
                    runConcurrently(
                            () -> joinGatheringUseCase.join(joinCommand(joinToken, 0)),
                            () -> joinGatheringAsHostUseCase.joinAsHost(hostCommand(1)));

            // then
            assertThat(failures)
                    .singleElement()
                    .isInstanceOf(GatheringParticipantCountExceedsLimitException.class);
            assertThat(participantCount()).isEqualTo(GatheringPolicy.MAX_PARTICIPANT_COUNT);
        }
    }

    // 남은 자리가 seats개가 되도록 참여자를 채운 모임을 저장하고, 그 모임의 원본 참여 토큰을 돌려준다.
    // 저장소는 부른 쪽의 트랜잭션을 전제로 하므로, 테스트 트랜잭션 없이 도는 준비도 트랜잭션 안에서 저장한다.
    private String saveGatheringWithSeats(int seats) {
        host = memberHelper.save(MemberFixture.KAKAO);

        return transactionTemplate.execute(
                _ -> {
                    Gathering gathering =
                            gatheringHelper.save(GatheringFixture.FRIDAY_DINNER, host.getId());
                    gatheringId = gathering.getId();
                    gatheringHelper.join(gathering, GatheringPolicy.MAX_PARTICIPANT_COUNT - seats);

                    return gatheringHelper.issueJoinToken(gathering);
                });
    }

    private JoinGatheringCommand joinCommand(String joinToken, int index) {
        return new JoinGatheringCommand(
                joinToken,
                "google-id-token-" + index,
                "동시참여" + index,
                concurrentPhoneNumber(index),
                true,
                GatheringPolicy.CONSENT_VERSION);
    }

    private JoinGatheringAsHostCommand hostCommand(int index) {
        return new JoinGatheringAsHostCommand(
                host.getId(),
                gatheringId,
                "모임장",
                concurrentPhoneNumber(index),
                true,
                GatheringPolicy.CONSENT_VERSION);
    }

    // 미리 채운 참여자의 휴대폰과 겹치지 않는 번호다.
    private String concurrentPhoneNumber(int index) {
        return String.format(Locale.ROOT, "0108000%04d", index);
    }

    private int participantCount() {
        return transactionTemplate.execute(
                _ ->
                        gatheringRepository
                                .findById(gatheringId)
                                .orElseThrow()
                                .getParticipants()
                                .size());
    }
}
