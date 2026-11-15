package com.symphonia.place.infrastructure.naver.client;

import com.symphonia.place.domain.client.PlaceSearchClient;
import com.symphonia.place.domain.exception.PlaceSearchLimitExceededException;
import com.symphonia.place.domain.exception.PlaceSearchUnavailableException;
import com.symphonia.place.domain.vo.Place;
import com.symphonia.place.domain.vo.PlaceSearchQuery;
import com.symphonia.place.infrastructure.naver.client.response.NaverLocalSearchResponse;
import com.symphonia.place.infrastructure.naver.config.properties.NaverLocalSearchProperties;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

// NAVER API HUB의 지역 검색을 부른다. 개발자센터의 검색 API는 신규 발급이 중단돼 쓰지 않는다.
// 이 API는 키워드만 받고 한 번에 최대 5건을 돌려준다. 호출 한도를 넘으면 429로 응답한다.
// sort=random은 정확도순이다. 리뷰 많은 순(comment)은 검색어와 상관없는 가게를 앞에 올려 쓰지 않는다.
@Slf4j
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
            // 본문 없는 200이나 204는 예외 없이 null로 읽히므로 검색 불가로 바꾼다.
            return Optional.ofNullable(
                            restClient
                                    .get()
                                    .uri(
                                            properties.uri() + SEARCH_PARAMETERS,
                                            query.text(),
                                            DISPLAY,
                                            SORT)
                                    .header(CLIENT_ID_HEADER, properties.clientId())
                                    .header(CLIENT_SECRET_HEADER, properties.clientSecret())
                                    .retrieve()
                                    .onStatus(
                                            status ->
                                                    status.isSameCodeAs(
                                                            HttpStatus.TOO_MANY_REQUESTS),
                                            (_, _) -> {
                                                throw new PlaceSearchLimitExceededException();
                                            })
                                    .body(NaverLocalSearchResponse.class))
                    .map(NaverLocalSearchResponse::toPlaces)
                    .orElseThrow(this::emptyBody);
        } catch (RestClientException e) {
            log.warn("맛집 검색 호출에 실패했습니다. 원인: {}", causeOf(e));
            throw new PlaceSearchUnavailableException();
        }
    }

    private PlaceSearchUnavailableException emptyBody() {
        log.warn("맛집 검색 응답 본문이 비어 있습니다.");
        return new PlaceSearchUnavailableException();
    }

    // 예외 메시지에는 요청 주소와 함께 지역명이 들어갈 수 있어 남기지 않는다.
    // 키 오설정(401), 타임아웃, 서버 오류를 운영 로그에서 구분할 수 있도록 상태 코드나 예외 종류만 남긴다.
    private String causeOf(RestClientException e) {
        return e instanceof RestClientResponseException response
                ? "HTTP " + response.getStatusCode().value()
                : e.getClass().getSimpleName();
    }
}
