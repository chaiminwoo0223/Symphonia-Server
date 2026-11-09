package com.symphonia.pairing.fixture;

import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.pairing.application.dto.query.RecommendPairingQuery;
import com.symphonia.pairing.domain.vo.AttendeeConstraint;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import java.util.Set;

public class RecommendPairingQueryFixture {
    private RelationshipType relationshipType = RelationshipType.FRIEND;
    // 분위기는 선택 입력이다. 기본값은 고르지 않은 상태로 두어 분위기 가산점이 다른 점수 규칙의 검증에 섞이지 않게 한다.
    private MoodType moodType;
    private Set<AttendeeConstraint> attendeeConstraints = Set.of();
    private Set<AllergyType> attendeeAllergies = Set.of();
    // 기본값은 로그인하지 않은 요청이다.
    private Long memberId;

    public RecommendPairingQueryFixture relationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
        return this;
    }

    public RecommendPairingQueryFixture moodType(MoodType moodType) {
        this.moodType = moodType;
        return this;
    }

    public RecommendPairingQueryFixture attendeeConstraints(
            AttendeeConstraint... attendeeConstraints) {
        this.attendeeConstraints = Set.of(attendeeConstraints);
        return this;
    }

    public RecommendPairingQueryFixture attendeeAllergies(AllergyType... attendeeAllergies) {
        this.attendeeAllergies = Set.of(attendeeAllergies);
        return this;
    }

    public RecommendPairingQueryFixture memberId(Long memberId) {
        this.memberId = memberId;
        return this;
    }

    public RecommendPairingQuery build() {
        return new RecommendPairingQuery(
                relationshipType, moodType, attendeeConstraints, attendeeAllergies, memberId);
    }
}
