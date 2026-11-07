package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.RegisterBankAccountCommand;
import com.symphonia.gathering.application.dto.result.LinkedParticipantResult;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.ParticipantBankAccount;
import com.symphonia.gathering.domain.exception.BankAccountAlreadyRegisteredException;
import com.symphonia.gathering.domain.exception.BankAccountConsentRequiredException;
import com.symphonia.gathering.domain.exception.InvalidBankAccountException;
import com.symphonia.gathering.domain.exception.LinkTokenNotFoundException;
import com.symphonia.gathering.domain.exception.OutdatedBankAccountConsentVersionException;
import com.symphonia.gathering.domain.exception.SettlementRecordingClosedException;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.vo.BankAccount;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantBankAccountFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@DisplayName("RegisterBankAccountService 단위 테스트")
class RegisterBankAccountServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long PAYER_ID = 20L;
    private static final Long LEE_ID = 21L;
    private static final Long PARK_ID = 22L;
    private static final String LINK_TOKEN = "link-token";
    private static final String HYPHENATED_ACCOUNT_NUMBER = "1234-5678-9012";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);
    private static final LocalDateTime DELETES_AT = NOW.plusDays(7);

    // 기록 기간은 모임을 만든 지 48시간이 되는 순간 닫힌다.
    // 닫히기 1초 전과 닫히는 순간을 경계로 쓴다.
    private static final LocalDateTime OPEN_PERIOD_OPENED_AT = NOW.minusHours(48).plusSeconds(1);
    private static final LocalDateTime CLOSED_PERIOD_OPENED_AT = NOW.minusHours(48);

    private RegisterBankAccountService registerBankAccountService;

    @Mock private GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    @Mock private GatheringRepository gatheringRepository;
    @Mock private ParticipantBankAccountRepository participantBankAccountRepository;

    @BeforeEach
    void setUp() {
        registerBankAccountService =
                new RegisterBankAccountService(
                        getLinkedParticipantUseCase,
                        gatheringRepository,
                        participantBankAccountRepository,
                        Clock.fixed(
                                NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }

    @Nested
    @DisplayName("register 메서드는")
    class Register {

        // 토큰이 없거나 모임의 파기 시각이 지났거나 데이터 키를 지운 경우다. 갈래별 판단은 ParticipantQueryServiceTest가 검증한다.
        @Nested
        @DisplayName("개인 링크 토큰으로 참여자를 찾지 못한 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 모임을 읽지 않으며 계좌를 저장하지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                given(getLinkedParticipantUseCase.getLinkedParticipant(LINK_TOKEN))
                        .willThrow(new LinkTokenNotFoundException());
                RegisterBankAccountCommand command = command(true);

                // when & then
                assertThatThrownBy(() -> registerBankAccountService.register(command))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                then(gatheringRepository).shouldHaveNoInteractions();
                then(participantBankAccountRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 된 경우")
        class WhenRecordingPeriodClosed {

            @Test
            @DisplayName("예외를 던지고 계좌를 저장하지 않는다")
            void shouldThrowSettlementRecordingClosedException() {
                // given
                givenGatheringOfPayer(CLOSED_PERIOD_OPENED_AT);
                RegisterBankAccountCommand command = command(true);

                // when & then
                assertThatThrownBy(() -> registerBankAccountService.register(command))
                        .isInstanceOf(SettlementRecordingClosedException.class);
                thenBankAccountNotSaved();
            }
        }

        @Nested
        @DisplayName("계좌를 보여 주는 데 동의하지 않은 경우")
        class WhenConsentNotAgreed {

            @Test
            @DisplayName("예외를 던지고 계좌를 저장하지 않는다")
            void shouldThrowBankAccountConsentRequiredException() {
                // given
                givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
                RegisterBankAccountCommand command = command(false);

                // when & then
                assertThatThrownBy(() -> registerBankAccountService.register(command))
                        .isInstanceOf(BankAccountConsentRequiredException.class);
                thenBankAccountNotSaved();
            }
        }

        @Nested
        @DisplayName("계좌 동의 안내 버전이 현재 버전과 다른 경우")
        class WhenConsentVersionOutdated {

            @Test
            @DisplayName("예외를 던지고 계좌를 저장하지 않는다")
            void shouldThrowOutdatedBankAccountConsentVersionException() {
                // given
                givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
                RegisterBankAccountCommand command =
                        new RegisterBankAccountCommand(
                                LINK_TOKEN,
                                ParticipantBankAccountFixture.KOOKMIN.getBankName(),
                                HYPHENATED_ACCOUNT_NUMBER,
                                true,
                                "2000-01-01");

                // when & then
                assertThatThrownBy(() -> registerBankAccountService.register(command))
                        .isInstanceOf(OutdatedBankAccountConsentVersionException.class);
                thenBankAccountNotSaved();
            }
        }

        @Nested
        @DisplayName("이미 계좌를 등록한 참여자인 경우")
        class WhenBankAccountAlreadyRegistered {

            @Test
            @DisplayName("예외를 던지고 계좌를 다시 저장하지 않는다")
            void shouldThrowBankAccountAlreadyRegisteredException() {
                // given
                givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
                given(participantBankAccountRepository.existsByParticipantId(PAYER_ID))
                        .willReturn(true);
                RegisterBankAccountCommand command = command(true);

                // when & then
                assertThatThrownBy(() -> registerBankAccountService.register(command))
                        .isInstanceOf(BankAccountAlreadyRegisteredException.class);
                thenBankAccountNotSaved();
            }
        }

        @Nested
        @DisplayName("계좌번호 형식이 틀린 경우")
        class WhenAccountNumberInvalid {

            @Test
            @DisplayName("예외를 던지고 계좌를 저장하지 않는다")
            void shouldThrowInvalidBankAccountException() {
                // given
                givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
                RegisterBankAccountCommand command =
                        new RegisterBankAccountCommand(
                                LINK_TOKEN,
                                ParticipantBankAccountFixture.KOOKMIN.getBankName(),
                                "12345",
                                true,
                                SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION);

                // when & then
                assertThatThrownBy(() -> registerBankAccountService.register(command))
                        .isInstanceOf(InvalidBankAccountException.class);
                thenBankAccountNotSaved();
            }
        }

        @Nested
        @DisplayName("등록할 수 있는 경우")
        class WhenRegistrable {

            @Test
            @DisplayName("하이픈을 지운 계좌와 동의 버전을 참여자가 속한 모임 id와 함께 저장한다")
            void shouldSaveNormalizedBankAccountWithGatheringId() {
                // given
                givenGatheringOfPayer(OPEN_PERIOD_OPENED_AT);
                given(participantBankAccountRepository.existsByParticipantId(PAYER_ID))
                        .willReturn(false);
                RegisterBankAccountCommand command = command(true);

                // when
                registerBankAccountService.register(command);

                // then
                ArgumentCaptor<ParticipantBankAccount> captor =
                        ArgumentCaptor.forClass(ParticipantBankAccount.class);
                then(participantBankAccountRepository)
                        .should()
                        .save(captor.capture(), eq(GATHERING_ID));
                ParticipantBankAccount saved = captor.getValue();
                assertThat(saved.getParticipantId()).isEqualTo(PAYER_ID);
                assertThat(saved.getBankAccount())
                        .isEqualTo(
                                new BankAccount(
                                        ParticipantBankAccountFixture.KOOKMIN.getBankName(),
                                        ParticipantBankAccountFixture.KOOKMIN.getAccountNumber()));
                assertThat(saved.getConsentVersion())
                        .isEqualTo(SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION);
            }
        }

        private RegisterBankAccountCommand command(boolean consentAgreed) {
            return new RegisterBankAccountCommand(
                    LINK_TOKEN,
                    ParticipantBankAccountFixture.KOOKMIN.getBankName(),
                    HYPHENATED_ACCOUNT_NUMBER,
                    consentAgreed,
                    SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION);
        }

        private void thenBankAccountNotSaved() {
            then(participantBankAccountRepository).should(never()).save(any(), anyLong());
        }
    }

    private void givenGatheringOfPayer(LocalDateTime openedAt) {
        given(getLinkedParticipantUseCase.getLinkedParticipant(LINK_TOKEN))
                .willReturn(new LinkedParticipantResult(PAYER_ID, DELETES_AT));
        given(gatheringRepository.findByParticipantId(PAYER_ID))
                .willReturn(Optional.of(gathering(openedAt)));
    }

    private Gathering gathering(LocalDateTime openedAt) {
        return GatheringFixture.FRIDAY_DINNER.createWithIdAndOpenedAt(
                GATHERING_ID,
                openedAt,
                List.of(
                        ParticipantFixture.KIM.createWithId(PAYER_ID),
                        ParticipantFixture.LEE.createWithId(LEE_ID),
                        ParticipantFixture.PARK.createWithId(PARK_ID)));
    }
}
