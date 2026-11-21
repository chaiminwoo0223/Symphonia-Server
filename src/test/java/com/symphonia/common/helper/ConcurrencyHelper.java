package com.symphonia.common.helper;

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

    // 작업을 각각 다른 스레드에서 동시에 실행하고, 실패한 작업의 예외만 모아 돌려준다.
    // 스트림은 지연 실행되므로 모든 작업을 제출한 뒤에 결과를 기다린다.
    // 먼저 뜬 스레드가 혼자 끝나 경합이 사라지지 않도록, 모든 스레드가 모인 뒤에 작업을 시작한다.
    public static List<Throwable> runConcurrently(Runnable... tasks) {
        CountDownLatch ready = new CountDownLatch(tasks.length);

        try (ExecutorService executor = Executors.newFixedThreadPool(tasks.length)) {
            List<CompletableFuture<Throwable>> futures =
                    Stream.of(tasks)
                            .map(
                                    task ->
                                            CompletableFuture.runAsync(
                                                            () -> {
                                                                ready.countDown();
                                                                await(ready);
                                                                task.run();
                                                            },
                                                            executor)
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

    // 요청이 같은 시점에 저장해야 경합을 확인할 수 있으므로, 제때 모이지 않으면 테스트를 실패시킨다.
    public static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
