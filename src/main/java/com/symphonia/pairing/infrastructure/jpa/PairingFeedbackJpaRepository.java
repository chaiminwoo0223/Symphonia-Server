package com.symphonia.pairing.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PairingFeedbackJpaRepository
        extends JpaRepository<PairingFeedbackJpaEntity, Long> {}
