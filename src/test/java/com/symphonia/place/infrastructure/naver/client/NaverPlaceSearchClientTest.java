package com.symphonia.place.infrastructure.naver.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.symphonia.UnitTest;
import com.symphonia.place.domain.exception.PlaceSearchLimitExceededException;
import com.symphonia.place.domain.exception.PlaceSearchUnavailableException;
import com.symphonia.place.domain.vo.Place;
import com.symphonia.place.domain.vo.PlaceSearchQuery;
import com.symphonia.place.infrastructure.naver.config.properties.NaverLocalSearchProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

@DisplayName("NaverPlaceSearchClient 단위 테스트")
class NaverPlaceSearchClientTest extends UnitTest {

    private static final String SEARCH_URI = "https://naver.test/search/v1/local";
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";
    private static final PlaceSearchQuery QUERY = PlaceSearchQuery.of("강남구", "호프");

    private MockRestServiceServer mockServer;
    private NaverPlaceSearchClient naverPlaceSearchClient;

    @BeforeEach
    void setUp() {
        NaverLocalSearchProperties properties =
                new NaverLocalSearchProperties(CLIENT_ID, CLIENT_SECRET, SEARCH_URI);
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        naverPlaceSearchClient = new NaverPlaceSearchClient(builder.build(), properties);
    }

    @Nested
    @DisplayName("search 메서드는")
    class Search {

        @Nested
        @DisplayName("네이버가 검색 결과를 돌려준 경우")
        class WhenSearchSucceeds {

            @Test
            @DisplayName("지역명과 검색어, 정확도순 5건, 인증 헤더로 호출한다")
            void shouldRequestWithQueryAndCredentials() {
                // given
                mockServer
                        .expect(method(HttpMethod.GET))
                        .andExpect(
                                queryParam(
                                        "query", UriUtils.encode("강남구 호프", StandardCharsets.UTF_8)))
                        .andExpect(queryParam("display", "5"))
                        .andExpect(queryParam("sort", "random"))
                        .andExpect(header("X-NCP-APIGW-API-KEY-ID", CLIENT_ID))
                        .andExpect(header("X-NCP-APIGW-API-KEY", CLIENT_SECRET))
                        .andRespond(withSuccess(body("[]"), MediaType.APPLICATION_JSON));

                // when
                naverPlaceSearchClient.search(QUERY);

                // then
                mockServer.verify();
            }

            @Test
            @DisplayName("응답 순서 그대로 Place를 반환하고 이름의 HTML을 풀고 좌표를 도 단위로 바꾼다")
            void shouldReturnPlacesInResponseOrderWithPlainNameAndDegrees() {
                // given
                String items =
                        """
                        [
                          {"title":"<b>호프</b>집 &amp; 치킨","link":"https://example.com/1",
                           "category":"술집>맥주,호프","description":"","telephone":"",
                           "address":"서울특별시 강남구 역삼동 1","roadAddress":"서울특별시 강남구 테헤란로 1",
                           "mapx":"1270278735","mapy":"375034081"},
                          {"title":"두번째 호프","link":"https://example.com/2",
                           "category":"술집>요리주점","description":"","telephone":"",
                           "address":"서울특별시 강남구 논현동 2","roadAddress":"서울특별시 강남구 논현로 2",
                           "mapx":"1270384976","mapy":"375240289"}
                        ]
                        """;
                mockServer
                        .expect(method(HttpMethod.GET))
                        .andRespond(withSuccess(body(items), MediaType.APPLICATION_JSON));

                // when
                List<Place> places = naverPlaceSearchClient.search(QUERY);

                // then
                assertThat(places)
                        .containsExactly(
                                new Place(
                                        "호프집 & 치킨",
                                        "술집>맥주,호프",
                                        "서울특별시 강남구 역삼동 1",
                                        "서울특별시 강남구 테헤란로 1",
                                        37.5034081,
                                        127.0278735,
                                        "https://example.com/1"),
                                new Place(
                                        "두번째 호프",
                                        "술집>요리주점",
                                        "서울특별시 강남구 논현동 2",
                                        "서울특별시 강남구 논현로 2",
                                        37.5240289,
                                        127.0384976,
                                        "https://example.com/2"));
            }

            @Test
            @DisplayName("결과가 없으면 빈 목록을 반환한다")
            void shouldReturnEmptyPlacesWhenNoItems() {
                // given
                mockServer
                        .expect(method(HttpMethod.GET))
                        .andRespond(withSuccess(body("[]"), MediaType.APPLICATION_JSON));

                // when
                List<Place> places = naverPlaceSearchClient.search(QUERY);

                // then
                assertThat(places).isEmpty();
            }

            @Test
            @DisplayName("좌표가 없는 가게는 지도에 찍을 수 없어 뺀다")
            void shouldExcludePlaceWithoutCoordinates() {
                // given
                String items =
                        """
                        [
                          {"title":"좌표 없는 호프","link":"","category":"술집>맥주,호프",
                           "address":"a","roadAddress":"b","mapx":"","mapy":""},
                          {"title":"좌표 있는 호프","link":"","category":"술집>맥주,호프",
                           "address":"a","roadAddress":"b","mapx":"1270278735","mapy":"375034081"}
                        ]
                        """;
                mockServer
                        .expect(method(HttpMethod.GET))
                        .andRespond(withSuccess(body(items), MediaType.APPLICATION_JSON));

                // when
                List<Place> places = naverPlaceSearchClient.search(QUERY);

                // then
                assertThat(places).extracting(Place::name).containsExactly("좌표 있는 호프");
            }

            @Test
            @DisplayName("이름이 없거나 좌표가 숫자가 아닌 가게는 뺀다")
            void shouldExcludePlaceWithBlankTitleOrNonNumericCoordinates() {
                // given
                String items =
                        """
                        [
                          {"title":"","link":"","category":"술집>맥주,호프",
                           "address":"a","roadAddress":"b","mapx":"1270278735","mapy":"375034081"},
                          {"title":"좌표가 이상한 호프","link":"","category":"술집>맥주,호프",
                           "address":"a","roadAddress":"b","mapx":"abc","mapy":"375034081"},
                          {"title":"정상 호프","link":"","category":"술집>맥주,호프",
                           "address":"a","roadAddress":"b","mapx":"1270278735","mapy":"375034081"}
                        ]
                        """;
                mockServer
                        .expect(method(HttpMethod.GET))
                        .andRespond(withSuccess(body(items), MediaType.APPLICATION_JSON));

                // when
                List<Place> places = naverPlaceSearchClient.search(QUERY);

                // then
                assertThat(places).extracting(Place::name).containsExactly("정상 호프");
            }

            @Test
            @DisplayName("태그를 먼저 지운 뒤 엔티티를 풀어 이름의 부등호가 태그로 지워지지 않는다")
            void shouldUnescapeEntitiesAfterRemovingTags() {
                // given
                String items =
                        """
                        [
                          {"title":"<b>호프</b> &lt;본점&gt;","link":"","category":"술집>맥주,호프",
                           "address":"a","roadAddress":"b","mapx":"1270278735","mapy":"375034081"}
                        ]
                        """;
                mockServer
                        .expect(method(HttpMethod.GET))
                        .andRespond(withSuccess(body(items), MediaType.APPLICATION_JSON));

                // when
                List<Place> places = naverPlaceSearchClient.search(QUERY);

                // then
                assertThat(places).extracting(Place::name).containsExactly("호프 <본점>");
            }

            @Test
            @DisplayName("응답에 items가 없으면 빈 목록을 반환한다")
            void shouldReturnEmptyPlacesWhenItemsMissing() {
                // given
                mockServer
                        .expect(method(HttpMethod.GET))
                        .andRespond(withSuccess("{\"total\":0}", MediaType.APPLICATION_JSON));

                // when
                List<Place> places = naverPlaceSearchClient.search(QUERY);

                // then
                assertThat(places).isEmpty();
            }
        }

        @Nested
        @DisplayName("네이버가 하루 호출 한도 초과(429)로 응답한 경우")
        class WhenLimitExceeded {

            @Test
            @DisplayName("PlaceSearchLimitExceededException을 던진다")
            void shouldThrowPlaceSearchLimitExceededException() {
                // given
                mockServer.expect(method(HttpMethod.GET)).andRespond(withTooManyRequests());

                // when & then
                assertThatThrownBy(() -> naverPlaceSearchClient.search(QUERY))
                        .isInstanceOf(PlaceSearchLimitExceededException.class);
            }
        }

        @Nested
        @DisplayName("네이버가 호출을 처리하지 못한 경우")
        class WhenNaverFails {

            @Test
            @DisplayName("서버 오류(5xx)면 PlaceSearchUnavailableException을 던진다")
            void shouldThrowPlaceSearchUnavailableExceptionWhenServerError() {
                // given
                mockServer.expect(method(HttpMethod.GET)).andRespond(withServerError());

                // when & then
                assertThatThrownBy(() -> naverPlaceSearchClient.search(QUERY))
                        .isInstanceOf(PlaceSearchUnavailableException.class);
            }

            @Test
            @DisplayName("인증키가 거부되면(401) PlaceSearchUnavailableException을 던진다")
            void shouldThrowPlaceSearchUnavailableExceptionWhenCredentialRejected() {
                // given
                mockServer.expect(method(HttpMethod.GET)).andRespond(withUnauthorizedRequest());

                // when & then
                assertThatThrownBy(() -> naverPlaceSearchClient.search(QUERY))
                        .isInstanceOf(PlaceSearchUnavailableException.class);
            }
        }
    }

    private String body(String items) {
        return "{\"lastBuildDate\":\"Sun, 15 Nov 2026 16:23:00 +0900\",\"total\":0,\"start\":1,"
                + "\"display\":5,\"items\":"
                + items
                + "}";
    }
}
