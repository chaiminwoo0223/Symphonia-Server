package com.symphonia.pairing.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PairingHelper {

    private final JdbcTemplate jdbcTemplate;

    // 트랜잭션 롤백 없이 커밋한 피드백과 그 피드백이 만든 집계, 선택 신호를 조합 하나 기준으로 모두 지운다.
    // 피드백 제출은 전역과 상황별 평점 집계를 함께 갱신하고, 모임 파기는 선택 신호를 더한다.
    public void deleteRowsOf(Long drinkId, Long anjuId) {
        for (String table :
                new String[] {
                    "pairing_situation_rating_summary",
                    "pairing_rating_summary",
                    "pairing_selection_summary",
                    "pairing_feedback"
                }) {
            jdbcTemplate.update(
                    "DELETE FROM " + table + " WHERE drink_id = ? AND anju_id = ?",
                    drinkId,
                    anjuId);
        }
    }
}
