package com.symphonia.gathering.infrastructure.jpa;

import static com.symphonia.gathering.helper.ConcurrencyHelper.await;
import static com.symphonia.gathering.helper.ConcurrencyHelper.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphonia.RepositoryTest;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.exception.BankAccountAlreadyRegisteredException;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantBankAccountFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.infrastructure.crypto.GatheringMasterKey;
import com.symphonia.gathering.infrastructure.crypto.config.GatheringCryptoConfig;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Import({
    ParticipantBankAccountRepositoryImpl.class,
    GatheringRepositoryImpl.class,
    GatheringDataKeyLoader.class,
    GatheringMasterKey.class,
    GatheringCryptoConfig.class
})
class ParticipantBankAccountRepositoryImplTest extends RepositoryTest {

    @Autowired private ParticipantBankAccountRepository participantBankAccountRepository;
    @Autowired private ParticipantBankAccountJpaRepository participantBankAccountJpaRepository;
    @Autowired private GatheringRepository gatheringRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long gatheringId;
    private Long participantId;

    @BeforeEach
    void setUp() {
        Gathering gathering =
                gatheringRepository.save(
                        GatheringFixture.FRIDAY_DINNER.createWithParticipants(
                                List.of(ParticipantFixture.KIM.create())));
        gatheringId = gathering.getId();
        participantId = gathering.getParticipants().getFirst().getId();
    }

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Nested
        @DisplayName("처음 등록하는 계좌인 경우")
        class WhenBankAccountIsNew {

            @Test
            @DisplayName("은행 이름과 계좌번호를 평문 없이 암호화해 동의 버전과 함께 저장한다")
            void shouldEncryptBankNameAndAccountNumber() {
                // given
                ParticipantBankAccountFixture fixture = ParticipantBankAccountFixture.KOOKMIN;

                // when
                participantBankAccountRepository.save(fixture.create(participantId), gatheringId);

                // then
                ParticipantBankAccountJpaEntity stored =
                        participantBankAccountJpaRepository.findById(participantId).orElseThrow();
                assertThat(new String(stored.getEncryptedBankName(), StandardCharsets.UTF_8))
                        .doesNotContain(fixture.getBankName());
                assertThat(new String(stored.getEncryptedAccountNumber(), StandardCharsets.UTF_8))
                        .doesNotContain(fixture.getAccountNumber());
                assertThat(stored.getConsentVersion())
                        .isEqualTo(SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION);
            }
        }

        @Nested
        @DisplayName("같은 참여자의 계좌를 다시 저장하는 경우")
        class WhenBankAccountAlreadyRegistered {

            @Test
            @DisplayName("BankAccountAlreadyRegisteredException이 발생한다")
            void shouldThrowBankAccountAlreadyRegisteredException() {
                // given: 동시 등록으로 Service의 중복 검사를 둘 다 통과한 상태
                participantBankAccountRepository.save(
                        ParticipantBankAccountFixture.KOOKMIN.create(participantId), gatheringId);
                entityManager.clear();

                // when & then
                assertThatThrownBy(
                                () ->
                                        participantBankAccountRepository.save(
                                                ParticipantBankAccountFixture.KOOKMIN.create(
                                                        participantId),
                                                gatheringId))
                        .isInstanceOf(BankAccountAlreadyRegisteredException.class);
            }
        }

        // 두 트랜잭션이 실제로 동시에 커밋해야 하므로 테스트 트랜잭션 없이 실행하고, 커밋된 행은 테스트가 끝나면 지운다.
        @Nested
        @DisplayName("같은 참여자의 계좌를 두 요청이 동시에 등록하는 경우")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        class WhenRegisteredConcurrently {

            @AfterEach
            void tearDown() {
                deleteGatheringRows();
            }

            @Test
            @DisplayName("한 요청만 저장되고 다른 요청은 BankAccountAlreadyRegisteredException이 발생한다")
            void shouldSaveOnlyOne() {
                // given
                CountDownLatch ready = new CountDownLatch(2);

                // when
                List<Throwable> failures =
                        runConcurrently(
                                () -> registerAfterBothReady(ready),
                                () -> registerAfterBothReady(ready));

                // then
                assertThat(failures)
                        .singleElement()
                        .isInstanceOf(BankAccountAlreadyRegisteredException.class);
                assertThat(
                                jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM participant_bank_account WHERE participant_id = ?",
                                        Long.class,
                                        participantId))
                        .isEqualTo(1L);
            }

            // 두 요청이 모두 Service의 중복 검사를 통과한 뒤 함께 저장하도록 맞춘다.
            private void registerAfterBothReady(CountDownLatch ready) {
                transactionTemplate.executeWithoutResult(
                        _ -> {
                            ready.countDown();
                            await(ready);
                            participantBankAccountRepository.save(
                                    ParticipantBankAccountFixture.KOOKMIN.create(participantId),
                                    gatheringId);
                        });
            }
        }
    }

    @Nested
    @DisplayName("existsByParticipantId 메서드는")
    class ExistsByParticipantId {

        @Test
        @DisplayName("계좌를 등록한 참여자면 true를 반환한다")
        void shouldReturnTrueWhenBankAccountRegistered() {
            // given
            participantBankAccountRepository.save(
                    ParticipantBankAccountFixture.KOOKMIN.create(participantId), gatheringId);

            // when
            boolean result = participantBankAccountRepository.existsByParticipantId(participantId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("계좌를 등록하지 않은 참여자면 false를 반환한다")
        void shouldReturnFalseWhenBankAccountNotRegistered() {
            // when
            boolean result = participantBankAccountRepository.existsByParticipantId(participantId);

            // then
            assertThat(result).isFalse();
        }
    }

    // 테스트 트랜잭션 없이 커밋한 모임의 행을 외래 키 순서대로 지운다.
    private void deleteGatheringRows() {
        jdbcTemplate.update(
                "DELETE FROM participant_bank_account WHERE participant_id IN (SELECT id FROM participant WHERE gathering_id = ?)",
                gatheringId);
        jdbcTemplate.update("DELETE FROM participant WHERE gathering_id = ?", gatheringId);
        jdbcTemplate.update("DELETE FROM gathering_data_key WHERE gathering_id = ?", gatheringId);
        jdbcTemplate.update("DELETE FROM gathering WHERE id = ?", gatheringId);
    }
}
