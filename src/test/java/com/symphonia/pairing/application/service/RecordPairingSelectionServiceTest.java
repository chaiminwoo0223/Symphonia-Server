package com.symphonia.pairing.application.service;

import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.pairing.application.dto.command.RecordPairingSelectionCommand;
import com.symphonia.pairing.domain.repository.PairingSelectionSummaryRepository;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingSituation;
import com.symphonia.pairing.domain.vo.RelationshipType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("RecordPairingSelectionService 단위 테스트")
class RecordPairingSelectionServiceTest extends UnitTest {

    private static final Long DRINK_ID = 1L;
    private static final Long ANJU_ID = 2L;

    @InjectMocks private RecordPairingSelectionService recordPairingSelectionService;

    @Mock private PairingSelectionSummaryRepository pairingSelectionSummaryRepository;

    @Nested
    @DisplayName("record 메서드는")
    class Record {

        @Test
        @DisplayName("고른 조합의 상황별 선택 횟수에 1을 더한다")
        void shouldAccumulateSelectionInSituation() {
            // given
            RecordPairingSelectionCommand command =
                    RecordPairingSelectionCommand.of(
                            DRINK_ID, ANJU_ID, RelationshipType.LOVER, MoodType.ROMANTIC);

            // when
            recordPairingSelectionService.record(command);

            // then
            then(pairingSelectionSummaryRepository)
                    .should()
                    .accumulate(
                            DRINK_ID,
                            ANJU_ID,
                            PairingSituation.of(RelationshipType.LOVER, MoodType.ROMANTIC));
        }

        @Test
        @DisplayName("분위기를 고르지 않았으면 분위기 없는 상황으로 더한다")
        void shouldAccumulateSelectionInSituationWithoutMoodWhenMoodNotChosen() {
            // given
            RecordPairingSelectionCommand command =
                    RecordPairingSelectionCommand.of(
                            DRINK_ID, ANJU_ID, RelationshipType.BOSS, null);

            // when
            recordPairingSelectionService.record(command);

            // then
            then(pairingSelectionSummaryRepository)
                    .should()
                    .accumulate(
                            DRINK_ID, ANJU_ID, PairingSituation.of(RelationshipType.BOSS, null));
        }
    }
}
