package com.symphonia.place.infrastructure.naver.client.response;

import com.symphonia.place.domain.vo.Place;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

public record NaverLocalSearchResponse(List<Item> items) {
    public List<Place> toPlaces() {
        return Stream.ofNullable(items)
                .flatMap(List::stream)
                .filter(Item::hasCoordinates)
                .map(Item::toPlace)
                .toList();
    }

    public record Item(
            String title,
            String category,
            String address,
            String roadAddress,
            String mapx,
            String mapy,
            String link) {
        // mapx는 경도, mapy는 위도에 10의 7제곱을 곱한 정수 문자열이다.
        private static final int COORDINATE_SCALE = 7;

        // 지도에 찍을 수 없는 가게는 보여 줄 수 없으므로 좌표가 없는 가게는 뺀다.
        public boolean hasCoordinates() {
            return StringUtils.hasText(mapx) && StringUtils.hasText(mapy);
        }

        public Place toPlace() {
            return new Place(
                    plainText(title),
                    category,
                    address,
                    roadAddress,
                    toDegrees(mapy),
                    toDegrees(mapx),
                    link);
        }

        // 제목에는 검색어를 강조한 <b> 태그와 &amp; 같은 HTML 엔티티가 섞여 온다.
        // 이름에 들어 있는 부등호가 태그로 지워지지 않도록 태그를 먼저 지우고 엔티티를 푼다.
        private static String plainText(String html) {
            return HtmlUtils.htmlUnescape(html.replaceAll("<[^>]*>", ""));
        }

        private static double toDegrees(String scaledDegrees) {
            return new BigDecimal(scaledDegrees).movePointLeft(COORDINATE_SCALE).doubleValue();
        }
    }
}
