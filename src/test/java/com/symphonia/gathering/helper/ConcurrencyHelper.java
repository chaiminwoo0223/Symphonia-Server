package com.symphonia.gathering.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class ConcurrencyHelper {

    private ConcurrencyHelper() {}

    // 두 작업을 서로 다른 스레드에서 동시에 실행하고, 실패한 작업의 예외만 모아 돌려준다.
    // 스트림은 지연 실행되므로 두 작업을 모두 제출한 뒤에 결과를 기다린다.
    public static List<Throwable> runConcurrently(Runnable first, Runnable second) {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<CompletableFuture<Throwable>> futures =
                    Stream.of(first, second)
                            .map(
                                    task ->
                                            CompletableFuture.runAsync(task, executor)
                                                    .handle((_, failure) -> failure)
                                                    .orTimeout(10, TimeUnit.SECONDS))
                            .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .map(Throwable::getCause)
                    .toList();
        }
    }

    // 두 요청이 같은 시점에 저장해야 경합을 확인할 수 있으므로, 제때 모이지 않으면 테스트를 실패시킨다.
    public static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
