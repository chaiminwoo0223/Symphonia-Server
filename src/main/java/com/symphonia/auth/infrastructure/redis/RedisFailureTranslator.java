package com.symphonia.auth.infrastructure.redis;

import com.symphonia.auth.domain.exception.TokenStoreUnavailableException;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;

// Redis 조회와 쓰기가 실패하면 도메인이 아는 예외로 바꿔 던진다. 장애 때 500이 아니라 503으로 응답하기 위해서다.
final class RedisFailureTranslator {
    private RedisFailureTranslator() {}

    public static <T> T translate(Supplier<T> command) {
        try {
            return command.get();
        } catch (DataAccessException e) {
            throw new TokenStoreUnavailableException(e);
        }
    }

    public static void translateVoid(Runnable command) {
        translate(
                () -> {
                    command.run();
                    return null;
                });
    }
}
