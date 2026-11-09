package com.symphonia.pairing.infrastructure.jpa;

import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import com.symphonia.pairing.domain.vo.CombinationId;
import com.symphonia.pairing.domain.vo.PairingRating;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PairingFeedbackRepositoryImpl implements PairingFeedbackRepository {
    private final PairingFeedbackJpaRepository pairingFeedbackJpaRepository;

    @Override
    public PairingFeedback save(PairingFeedback pairingFeedback) {
        PairingFeedbackJpaEntity savedEntity =
                pairingFeedbackJpaRepository.save(PairingFeedbackJpaEntity.from(pairingFeedback));

        return savedEntity.toDomain();
    }

    // 네이티브 INSERT는 JPA Auditing을 거치지 않으므로, Auditing과 같은 LocalDateTime.now()로 생성과 수정 시각을 채운다.
    @Override
    public boolean saveIfAbsent(PairingFeedback pairingFeedback) {
        LocalDateTime now = LocalDateTime.now();

        return pairingFeedbackJpaRepository.insertIfAbsent(
                        pairingFeedback.getMemberId(),
                        pairingFeedback.getDrinkId(),
                        pairingFeedback.getAnjuId(),
                        pairingFeedback.getRelationshipType().name(),
                        pairingFeedback.getMoodType() == null
                                ? null
                                : pairingFeedback.getMoodType().name(),
                        pairingFeedback.getRating().name(),
                        now)
                == 1;
    }

    @Override
    public Optional<PairingFeedback> findByMemberIdAndCombinationForUpdate(
            Long memberId, Long drinkId, Long anjuId) {
        return pairingFeedbackJpaRepository
                .findByMemberIdAndCombinationForUpdate(memberId, drinkId, anjuId)
                .map(PairingFeedbackJpaEntity::toDomain);
    }

    @Override
    public Set<CombinationId> findDislikedCombinationsByMemberId(Long memberId) {
        return Set.copyOf(
                pairingFeedbackJpaRepository.findCombinationsByMemberIdAndRating(
                        memberId, PairingRating.DISLIKE));
    }
}
