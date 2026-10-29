package com.symphonia.gathering.infrastructure.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(GatheringJoinTokenRepositoryImpl.class)
class GatheringJoinTokenRepositoryImplTest extends RepositoryTest {

    private static final String VALUE = "join-token-value";

    @Autowired private GatheringJoinTokenRepository gatheringJoinTokenRepository;
    @Autowired private GatheringJoinTokenJpaRepository gatheringJoinTokenJpaRepository;
    @Autowired private GatheringJpaRepository gatheringJpaRepository;

    private Long gatheringId;

    @BeforeEach
    void setUp() {
        gatheringId =
                gatheringJpaRepository
                        .save(GatheringJpaEntity.from(GatheringFixture.FRIDAY_DINNER.create()))
                        .getId();
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("참여 토큰을 저장한다")
        void shouldPersistJoinToken() {
            // when
            gatheringJoinTokenRepository.save(VALUE, gatheringId);

            // then
            assertThat(gatheringJoinTokenRepository.findGatheringIdByValue(VALUE))
                    .contains(gatheringId);
        }

        @Test
        @DisplayName("원본 토큰 값이 아니라 해시된 값을 저장한다")
        void shouldPersistHashedValueInsteadOfRawValue() {
            // when
            gatheringJoinTokenRepository.save(VALUE, gatheringId);

            // then
            assertThat(gatheringJoinTokenJpaRepository.findByTokenHash(VALUE)).isEmpty();
        }

        @Test
        @DisplayName("같은 모임에 다시 저장하면 기존 토큰을 무효로 만든다")
        void shouldInvalidatePreviousTokenWhenReissued() {
            // given
            gatheringJoinTokenRepository.save(VALUE, gatheringId);

            // when
            gatheringJoinTokenRepository.save("reissued-join-token-value", gatheringId);

            // then
            assertThat(gatheringJoinTokenRepository.findGatheringIdByValue(VALUE)).isEmpty();
            assertThat(
                            gatheringJoinTokenRepository.findGatheringIdByValue(
                                    "reissued-join-token-value"))
                    .contains(gatheringId);
        }
    }

    @Nested
    @DisplayName("findGatheringIdByValue 메서드는")
    class FindGatheringIdByValue {

        @Test
        @DisplayName("존재하지 않는 값이면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenValueNotExists() {
            // when
            Optional<Long> result = gatheringJoinTokenRepository.findGatheringIdByValue("unknown");

            // then
            assertThat(result).isEmpty();
        }
    }
}
