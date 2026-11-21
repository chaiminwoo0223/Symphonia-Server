package com.symphonia.gathering.application.service;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.DeleteGatheringCommand;
import com.symphonia.gathering.application.usecase.PurgeGatheringUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("DeleteGatheringService 단위 테스트")
class DeleteGatheringServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long HOST_MEMBER_ID = 1L;

    @InjectMocks private DeleteGatheringService deleteGatheringService;

    @Mock private PurgeGatheringUseCase purgeGatheringUseCase;

    @Nested
    @DisplayName("delete 메서드는")
    class Delete {

        @Nested
        @DisplayName("데이터 키를 지운 경우")
        class WhenDataKeyDiscarded {

            @Test
            @DisplayName("데이터 키를 지운 뒤에 모임의 나머지 행을 선택 신호 없이 지운다")
            void shouldPurgeAfterDiscardingDataKey() {
                // given
                given(
                                purgeGatheringUseCase.discardDataKeyOfHostedGathering(
                                        HOST_MEMBER_ID, GATHERING_ID))
                        .willReturn(true);

                // when
                deleteGatheringService.delete(
                        DeleteGatheringCommand.of(HOST_MEMBER_ID, GATHERING_ID));

                // then
                InOrder inOrder = inOrder(purgeGatheringUseCase);
                inOrder.verify(purgeGatheringUseCase)
                        .discardDataKeyOfHostedGathering(HOST_MEMBER_ID, GATHERING_ID);
                inOrder.verify(purgeGatheringUseCase).purgeWithoutSelectionSignal(GATHERING_ID);
            }
        }

        @Nested
        @DisplayName("이미 지운 모임인 경우")
        class WhenGatheringAlreadyDeleted {

            @Test
            @DisplayName("행을 지우지 않고 성공으로 끝낸다")
            void shouldNotPurge() {
                // given
                given(
                                purgeGatheringUseCase.discardDataKeyOfHostedGathering(
                                        HOST_MEMBER_ID, GATHERING_ID))
                        .willReturn(false);

                // when
                deleteGatheringService.delete(
                        DeleteGatheringCommand.of(HOST_MEMBER_ID, GATHERING_ID));

                // then
                then(purgeGatheringUseCase).should(never()).purgeWithoutSelectionSignal(anyLong());
            }
        }
    }
}
