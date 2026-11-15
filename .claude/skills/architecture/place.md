# place 맛집 조회 규칙

`architecture` 스킬의 하위 문서다. 언제 읽는지는 `SKILL.md`의 하위 문서 표를 참고한다. 근거, 택하지 않은 안, 표본 호출 결과는 `docs/decisions/place.md`에 있다.

## 책임과 의존

- `place`는 안주를 곁들일 가게를 외부 검색으로 찾아 돌려준다.
- 의존 방향은 `place → catalog` 한 방향이다. `pairing`과 `gathering`에는 의존하지 않는다.
- 안주의 맛집 검색어는 catalog의 `GetAnjuPlaceKeywordUseCase`로 읽는다.
- 외부 검색은 `domain`의 `PlaceSearchClient`로 선언하고 `infrastructure.naver`의 `NaverPlaceSearchClient`가 구현한다.

## 조회 API

- `GET /api/v1/places/recommendations`는 지역명(`region`)과 안주 id(`anjuId`) 하나를 받고, 로그인 없이 호출할 수 있다.
- 안주 이름 문자열은 받지 않는다. 임의의 검색어로 외부 검색의 호출 한도를 쓰지 못하게 하기 위해서다.
- 지역명은 한글, 숫자, 공백만 30자까지 받는다. 서버는 지역명을 검색어에만 쓰고, 저장하거나 로그에 남기지 않는다.
- 서버는 사용자 좌표를 받지 않는다. 기기 위치를 지역명으로 바꾸는 일은 클라이언트가 한다.
- 검색어는 `"{지역명} {안주의 맛집 검색어}"`이고 요청마다 한 번 호출한다. 지역명의 연속 공백은 하나로 정리한다.
- 응답은 `source`와 가게 목록이다. 가게는 외부 검색이 돌려준 순서 그대로 담고, 중복 제거, 재정렬, 다른 검색어 결과로의 보충을 하지 않는다. 결과가 없으면 빈 목록이다.
- 가게는 이름, 분류, 지번 주소, 도로명 주소, 위도, 경도, 원본 링크를 갖는다. 클라이언트는 `source`와 `link`를 화면에 표시한다.

## 외부 검색

- NAVER API HUB의 지역 검색을 쓴다. 개발자센터의 검색 API는 쓰지 않는다.
- 키는 `NAVER_LOCAL_SEARCH_CLIENT_ID`와 `NAVER_LOCAL_SEARCH_CLIENT_SECRET`으로 주입한다.
- 한 번에 5건을 정확도순(`sort=random`)으로 받는다. 리뷰순(`comment`)은 쓰지 않는다.
- 좌표는 도에 10의 7제곱을 곱한 정수 문자열이라 도 단위로 바꾼다. 이름이 없거나 좌표를 숫자로 읽을 수 없는 가게는 뺀다.
- 이름의 HTML 태그와 엔티티는 풀어서 담는다.
- 429는 `PlaceSearchLimitExceededException`, 그 밖의 호출 실패는 `PlaceSearchUnavailableException`으로 바꾼다. 둘 다 503이고 코드는 `PLACE_SEARCH_LIMIT_EXCEEDED`, `PLACE_SEARCH_UNAVAILABLE`이다.
- 호출은 `global.config.RestClientConfig`의 공유 `RestClient`로 한다. 호출 실패 로그에는 상태 코드나 예외 종류만 남기고, 지역명이 들어갈 수 있는 예외 메시지는 남기지 않는다.

## 저장과 캐시

- 가게 정보는 DB에 저장하지 않는다. 약관이 별도 DB에 복사하는 것을 금지한다.
- 검색 결과는 Redis에 10분만 담는다. 키는 `place:v1:` 뒤에 검색어의 SHA-256이다. 결과가 없던 검색도 담는다.
- Redis 장애와 깨진 값은 삼키고 외부 검색을 부른다. 외부 검색이 실패하면 캐시에 담지 않는다.

## 안주의 맛집 검색어

- 안주 이름을 기본 검색어로 쓴다. 이름으로 맞는 가게를 찾지 못하는 안주만 `anjus.csv`의 `place_keyword` 열에 지정한다(`persistence.md`).
- 과일, 견과, 치즈 단품, 디저트처럼 가게 메뉴가 아닌 안주는 곁들이는 술집 유형(`와인바`, `호프`, `이자카야`, `전통주점`)으로 지정한다.
