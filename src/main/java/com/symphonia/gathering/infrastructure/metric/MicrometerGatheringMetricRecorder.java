package com.symphonia.gathering.infrastructure.metric;

import com.symphonia.gathering.application.event.ParticipantJoinedEvent.JoinType;
import com.symphonia.gathering.application.recorder.GatheringMetricRecorder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

// 첫 건이 생기기 전에도 지표가 0으로 보이도록, 카운터는 시작할 때 모두 등록한다.
@Component
public class MicrometerGatheringMetricRecorder implements GatheringMetricRecorder {
    private final Counter gatheringOpenedCounter;
    private final Map<JoinType, Counter> participantJoinedCounters;
    private final Counter settlementRecordedCounter;
    private final Counter sharesSentCounter;

    public MicrometerGatheringMetricRecorder(MeterRegistry meterRegistry) {
        gatheringOpenedCounter =
                Counter.builder("symphonia.gathering.opened")
                        .description("새로 연 모임 수")
                        .register(meterRegistry);
        participantJoinedCounters =
                Arrays.stream(JoinType.values())
                        .collect(
                                Collectors.toMap(
                                        Function.identity(),
                                        type -> participantJoinedCounter(meterRegistry, type)));
        settlementRecordedCounter =
                Counter.builder("symphonia.gathering.settlement.recorded")
                        .description("새로 기록한 정산 건 수")
                        .register(meterRegistry);
        sharesSentCounter =
                Counter.builder("symphonia.gathering.share.sent.requests")
                        .description("분담금을 하나라도 보냄으로 바꾼 \"보냈어요\" 요청 수")
                        .register(meterRegistry);
    }

    @Override
    public void recordGatheringOpened() {
        gatheringOpenedCounter.increment();
    }

    @Override
    public void recordParticipantJoined(JoinType type) {
        participantJoinedCounters.get(type).increment();
    }

    @Override
    public void recordSettlementRecorded() {
        settlementRecordedCounter.increment();
    }

    @Override
    public void recordSharesSent() {
        sharesSentCounter.increment();
    }

    private static Counter participantJoinedCounter(MeterRegistry meterRegistry, JoinType type) {
        return Counter.builder("symphonia.gathering.participant.joined")
                .description("모임에 참여한 참여자 수")
                .tag("type", type.name().toLowerCase(Locale.ROOT))
                .register(meterRegistry);
    }
}
