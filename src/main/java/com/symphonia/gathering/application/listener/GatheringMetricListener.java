package com.symphonia.gathering.application.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.symphonia.gathering.application.event.GatheringOpenedEvent;
import com.symphonia.gathering.application.event.ParticipantJoinedEvent;
import com.symphonia.gathering.application.event.SettlementRecordedEvent;
import com.symphonia.gathering.application.event.SharesSentEvent;
import com.symphonia.gathering.application.recorder.GatheringMetricRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// 버전 충돌처럼 커밋 시점에 실패하는 요청은 세지 않도록, 커밋이 끝난 뒤에 센다.
@Component
@RequiredArgsConstructor
public class GatheringMetricListener {
    private final GatheringMetricRecorder gatheringMetricRecorder;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(GatheringOpenedEvent event) {
        gatheringMetricRecorder.recordGatheringOpened();
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(ParticipantJoinedEvent event) {
        gatheringMetricRecorder.recordParticipantJoined(event.type());
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(SettlementRecordedEvent event) {
        gatheringMetricRecorder.recordSettlementRecorded();
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(SharesSentEvent event) {
        gatheringMetricRecorder.recordSharesSent();
    }
}
