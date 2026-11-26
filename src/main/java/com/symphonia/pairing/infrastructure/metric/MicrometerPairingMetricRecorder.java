package com.symphonia.pairing.infrastructure.metric;

import com.symphonia.pairing.application.event.PairingFeedbackSubmittedEvent.SubmitterType;
import com.symphonia.pairing.application.recorder.PairingMetricRecorder;
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
public class MicrometerPairingMetricRecorder implements PairingMetricRecorder {
    private final Counter recommendedCounter;
    private final Map<SubmitterType, Counter> feedbackSubmittedCounters;

    public MicrometerPairingMetricRecorder(MeterRegistry meterRegistry) {
        recommendedCounter =
                Counter.builder("symphonia.pairing.recommended")
                        .description("성공한 페어링 추천 요청 수")
                        .register(meterRegistry);
        feedbackSubmittedCounters =
                Arrays.stream(SubmitterType.values())
                        .collect(
                                Collectors.toMap(
                                        Function.identity(),
                                        type -> feedbackSubmittedCounter(meterRegistry, type)));
    }

    @Override
    public void recordRecommended() {
        recommendedCounter.increment();
    }

    @Override
    public void recordFeedbackSubmitted(SubmitterType type) {
        feedbackSubmittedCounters.get(type).increment();
    }

    private static Counter feedbackSubmittedCounter(
            MeterRegistry meterRegistry, SubmitterType type) {
        return Counter.builder("symphonia.pairing.feedback.submitted")
                .description("새로 제출한 페어링 피드백 수")
                .tag("type", type.name().toLowerCase(Locale.ROOT))
                .register(meterRegistry);
    }
}
