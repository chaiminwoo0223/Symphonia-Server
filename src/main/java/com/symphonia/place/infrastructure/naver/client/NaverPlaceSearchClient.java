package com.symphonia.place.infrastructure.naver.client;

import com.symphonia.place.domain.client.PlaceSearchClient;
import com.symphonia.place.domain.exception.PlaceSearchLimitExceededException;
import com.symphonia.place.domain.exception.PlaceSearchUnavailableException;
import com.symphonia.place.domain.vo.Place;
import com.symphonia.place.domain.vo.PlaceSearchQuery;
import com.symphonia.place.infrastructure.naver.client.response.NaverLocalSearchResponse;
import com.symphonia.place.infrastructure.naver.config.properties.NaverLocalSearchProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// NAVER API HUB의 지역 검색을 부른다. 개발자센터의 검색 API는 신규 발급이 중단돼 쓰지 않는다.
// 이 API는 키워드만 받고 한 번에 최대 5건을 돌려준다. 호출 한도를 넘으면 429로 응답한다.
// sort=random은 정확도순이다. 리뷰 많은 순(comment)은 검색어와 상관없는 가게를 앞에 올려 쓰지 않는다.
@Component
@RequiredArgsConstructor
public class NaverPlaceSearchClient implements PlaceSearchClient {
    private static final String CLIENT_ID_HEADER = "X-NCP-APIGW-API-KEY-ID";
    private static final String CLIENT_SECRET_HEADER = "X-NCP-APIGW-API-KEY";
    private static final String SEARCH_PARAMETERS = "?query={query}&display={display}&sort={sort}";
    private static final int DISPLAY = 5;
    private static final String SORT = "random";

    private final RestClient restClient;
    private final NaverLocalSearchProperties properties;

    @Override
    public List<Place> search(PlaceSearchQuery query) {
        try {
            return restClient
                    .get()
                    .uri(properties.uri() + SEARCH_PARAMETERS, query.text(), DISPLAY, SORT)
                    .header(CLIENT_ID_HEADER, properties.clientId())
                    .header(CLIENT_SECRET_HEADER, properties.clientSecret())
                    .retrieve()
                    .onStatus(
                            status -> status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS),
                            (_, _) -> {
                                throw new PlaceSearchLimitExceededException();
                            })
                    .requiredBody(NaverLocalSearchResponse.class)
                    .toPlaces();
        } catch (RestClientException e) {
            throw new PlaceSearchUnavailableException();
        }
    }
}
