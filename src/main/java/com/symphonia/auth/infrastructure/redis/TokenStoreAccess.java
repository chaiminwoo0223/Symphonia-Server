package com.symphonia.auth.infrastructure.redis;

import com.symphonia.auth.domain.exception.TokenStoreUnavailableException;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;

// Redis 조회와 쓰기가 실패하면 도메인이 아는 예외로 바꿔 던진다. 장애 때 500이 아니라 503으로 응답하기 위해서다.
final class TokenStoreAccess {
    private TokenStoreAccess() {}

    static <T> T call(Supplier<T> command) {
        try {
            return command.get();
        } catch (DataAccessException e) {
            throw new TokenStoreUnavailableException(e);
        }
    }

    static void run(Runnable command) {
        call(
                () -> {
                    command.run();
                    return null;
                });
    }
}
