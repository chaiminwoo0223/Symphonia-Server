package com.symphonia.gathering.application.service;

import static com.symphonia.common.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.IntegrationTest;
import com.symphonia.gathering.application.dto.command.OpenGatheringCommand;
import com.symphonia.gathering.application.dto.result.GatheringJoinTokenResult;
import com.symphonia.gathering.application.usecase.OpenGatheringUseCase;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.helper.GatheringHelper;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 앱 화면에서 열기 버튼을 두 번 누르거나 응답이 늦어 재시도가 겹쳐도 모임이 하나만 만들어지는지 실제 DB로 확인한다(#217).
// 트랜잭션 롤백 없이 커밋하므로 직접 지운다.
@DisplayName("모임 열기 동시성 통합 테스트")
class GatheringOpenConcurrencyIntegrationTest extends IntegrationTest {

    private static final int CONCURRENT_OPENS = 5;

    @Autowired private OpenGatheringUseCase openGatheringUseCase;
    @Autowired private GatheringHelper gatheringHelper;
    @Autowired private MemberHelper memberHelper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Member host;
    private List<GatheringJoinTokenResult> results;

    @AfterEach
    void deleteCommittedRows() {
        results.stream()
                .map(GatheringJoinTokenResult::gatheringId)
                .distinct()
                .forEach(gatheringHelper::deleteRows);
        memberRepository.delete(host);
    }

    @Nested
    @DisplayName("open 메서드는")
    class Open {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("같은 요청 id로 동시에 열어도 모임을 하나만 만들고 모두 그 모임을 돌려준다")
        void shouldCreateOneGatheringWhenOpenedConcurrentlyWithSameClientRequestId() {
            // given
            host = memberHelper.save(MemberFixture.KAKAO);
            UUID clientRequestId = UUID.randomUUID();
            results = new CopyOnWriteArrayList<>();
            Runnable[] opens =
                    IntStream.range(0, CONCURRENT_OPENS)
                            .<Runnable>mapToObj(
                                    _ ->
                                            () ->
                                                    results.add(
                                                            openGatheringUseCase.open(
                                                                    command(clientRequestId))))
                            .toArray(Runnable[]::new);

            // when
            List<Throwable> failures = runConcurrently(opens);

            // then
            assertThat(failures).isEmpty();
            assertThat(results)
                    .hasSize(CONCURRENT_OPENS)
                    .extracting(GatheringJoinTokenResult::gatheringId)
                    .containsOnly(results.getFirst().gatheringId());
            assertThat(countGatherings()).isOne();
        }
    }

    private OpenGatheringCommand command(UUID clientRequestId) {
        return new OpenGatheringCommand(
                host.getId(), clientRequestId, GatheringFixture.FRIDAY_DINNER.getName(), null);
    }

    private Integer countGatherings() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gathering WHERE host_member_id = ?",
                Integer.class,
                host.getId());
    }
}
