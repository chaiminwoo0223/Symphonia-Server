package com.symphonia.common.audit;

import java.util.Optional;

public interface ActorIdResolver {
    Optional<String> resolve();
}
