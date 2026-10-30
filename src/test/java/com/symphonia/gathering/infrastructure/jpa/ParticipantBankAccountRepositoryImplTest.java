package com.symphonia.gathering.infrastructure.jpa;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

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
}
