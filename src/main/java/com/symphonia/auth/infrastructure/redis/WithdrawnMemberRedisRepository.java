package com.symphonia.auth.infrastructure.redis;

import org.springframework.data.repository.CrudRepository;

public interface WithdrawnMemberRedisRepository extends CrudRepository<WithdrawnMember, String> {}
