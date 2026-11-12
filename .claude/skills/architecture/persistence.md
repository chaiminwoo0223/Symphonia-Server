# 영속성 (PostgreSQL / pgvector)

`architecture` 스킬의 하위 문서다. 언제 읽는지는 `SKILL.md`의 하위 문서 표를 참고한다. 근거, 택하지 않은 안, 재검토 조건, 데이터 선정 이력은 `docs/decisions/persistence.md`에 있다.

## DB와 이미지

- DB는 PostgreSQL 17이다. 로컬 compose와 Testcontainers 모두 `pgvector/pgvector:pg17` 이미지를 쓴다.
- `compose.yaml`의 `postgres` 서비스에 `org.springframework.boot.service-connection=postgres` 라벨을 유지한다. 없으면 `spring-boot-docker-compose`가 datasource를 연결하지 못한다. 테스트는 이 문제를 잡지 못하므로 이미지를 바꾸면 `bootRun`으로 직접 확인한다.
- RabbitMQ는 compose와 Testcontainers 모두 `rabbitmq:4.3-management` 이미지를 쓴다.
- `CREATE EXTENSION IF NOT EXISTS vector`는 vector 컬럼을 도입하는 이슈의 마이그레이션에서 실행한다.

## 맛과 분위기 값의 저장

- `FlavorProfile`(int 5컬럼)은 int 컬럼 `*Embeddable`로 두고 vector 컬럼으로 바꾸지 않는다. 점수는 자바에서 계산한다.
- ANN 인덱스(ivfflat/hnsw)는 두지 않는다.
- vector 컬럼으로 전환하게 되면 도메인 VO는 그대로 두고 `*Embeddable`과 `*JpaEntity`에만 `@JdbcTypeCode(SqlTypes.VECTOR)`와 `@Array(length = 축 개수)`로 매핑한다.
- 유사도 축을 바꾸면 카탈로그 전체의 `MATCH_THRESHOLD` 이상 매칭 비율을 다시 계산해 임계값을 확인한다.

## Flyway와 스키마

- 스키마와 시드 데이터는 Flyway 마이그레이션(`db/migration/V{n}__설명.sql`)으로만 바꾼다. `ddl-auto`는 `validate`다.
- 이미 적용된 V 파일은 수정하지 않는다. 주석의 옛 이름도 고치지 않는다. 스키마 변경, enum 값 추가, 시드 추가는 모두 새 버전 파일로 한다.
- 시드는 id를 명시해 넣고, 파일 끝에서 `setval(pg_get_serial_sequence(...), MAX(id))`로 시퀀스를 보정한다.
- 테이블 이름은 `@Table(name)`으로 지정해 `_jpa_entity` 접미사가 스키마에 드러나지 않게 한다.
- 옛 `*_jpa_entity` 테이블이 남은 DB(로컬 compose 등)는 볼륨을 초기화한 뒤 기동한다.
- `@Enumerated(STRING)` 컬럼은 `varchar(255)`와 허용 값 `check` 제약으로 만든다. enum 값을 추가하면 새 마이그레이션에서 `check` 제약을 고친다.
- 값이 늘어날 enum의 `check` 제약에는 `ck_<테이블>_<컬럼>` 이름을 붙여 후속 마이그레이션이 이름으로 DROP한 뒤 다시 ADD한다.
- 레포에 두는 단독 `.sql` 파일은 Flyway 마이그레이션과 테스트 픽스처(`src/test/resources/sql`)뿐이다.
- 테스트 DB에도 시드가 들어가므로, 카탈로그 내용에 기대는 테스트는 `@Sql("/sql/clear-catalog.sql")`로 카탈로그를 비우고 시작한다.

## 인덱스와 제약

- 인덱스와 제약은 마이그레이션 스크립트에 선언한다. PostgreSQL은 FK 컬럼을 자동으로 인덱싱하지 않는다.
- FK 제약이나 새 조회 조건 컬럼이 생기면 인덱스가 필요한지 함께 검토한다. 조회 조건이 없는 FK 컬럼(`pairing_feedback`의 drink, anju, `drink.drink_style_id`)에는 인덱스를 두지 않는다.
- `member`의 `(social_provider, social_id)`에 unique 제약 `uk_member_social_login`을 둔다.
- `drink.name`에는 유니크 제약을 걸지 않는다. `pairing_feedback.member_id`처럼 도메인 간 참조에는 FK를 걸지 않는다.
- 제약 위반은 `*RepositoryImpl`에서 제약 이름으로 구분해 도메인 예외로 바꾼다(예: `uk_member_social_login` → `MemberAlreadyExistsException`). 다른 무결성 위반은 바꾸지 않고 그대로 전파한다.

## 동시 INSERT

- 동시에 들어온 요청 중 한 행만 남겨야 하는 INSERT는 행의 성격으로 방식을 나눈다.
- 도메인 상태 없이 있는지만 뜻하는 기록용 행은 네이티브 `INSERT ... ON CONFLICT DO NOTHING`으로 넣고, 넣었는지를 boolean으로 돌려준다. 중복인지는 Service가 반환값으로 판단한다(예: `participant_pairing_feedback`, `notification_schedule`, `pairing_feedback` 첫 제출).
- 변환, 암호화, 연관이 있는 엔티티는 JPA `saveAndFlush`로 저장하고, 제약 위반을 제약 이름으로 구분해 도메인 예외로 바꾼다(예: 계좌, 회원 소셜 로그인, 참여자 휴대폰).

## 음료 모델

- `Drink`는 제품 단위이고, 분류(`DrinkCategory`)와 기본 맛은 `DrinkStyle`이 갖는다. `Drink`는 `DrinkStyle`을 `drinkStyleId`로 참조하는 별도 애그리거트다.
- 추천 점수는 `Drink`의 `flavorProfile`로 계산한다. 스타일의 기본 맛은 적재할 때 제품 맛의 초기값으로만 쓴다.
- `Drink`, `DrinkStyle`, `Anju`는 `(source, external_id)` 유니크 제약으로 식별하고, `external_id`는 `NOT NULL`이다. `DrinkStyle`은 `name`에도 유니크 제약이 있다.
- `DrinkSource`는 `SEED`, `CURATED`, `X_WINES`이고, `DrinkStyleSource`는 `SEED`, `BJCP`, `X_WINES`, `AnjuSource`는 `SEED`, `K_FIND`다.
- 시드 행은 `pairing_feedback`이 FK로 참조하므로 지우지 않고 id를 유지한 채 적재가 갱신한다. 시드 안주는 마이그레이션이 소유하므로 적재가 지우지 않는다.

## 카탈로그 적재

- 적재는 Spring Boot 적재 러너로 한다. `./gradlew importCatalog`가 `import.catalog.enabled=true`를 넘기면 `catalog.presentation.runner`의 `ImportCatalogRunner`가 BJCP 스타일, 국내 유통 맥주, X-Wines 와인, 안주 적재 UseCase를 차례로 호출한다. 일반 기동에서는 `@ConditionalOnProperty`로 러너 빈이 만들어지지 않는다. 적재 전용 프로필과 Spring Batch는 쓰지 않는다.
- 스타일을 먼저 적재한다. UseCase마다 트랜잭션이 따로라 스타일 적재가 먼저 커밋된다.
- 맛은 규칙표로만 계산한다. 사람이 제품마다 맛 값을 적지 않는다. 규칙은 도메인의 `FlavorRule`, `WineFlavorRule`, `AnjuRule`에, 값은 `src/main/resources/import/rules/`의 CSV 규칙표에 둔다.
- 파일 읽기는 `catalog.domain.reader` 인터페이스를 `catalog.infrastructure.reader`의 `*Reader` 구현체가 구현한다.
- 맛 계산 결과는 `Import*Service` 단위 테스트가 실제 규칙으로 검증한다. 도메인 테스트는 두지 않는다.
- 다시 적재할 때 덮어쓸 필드는 엔티티의 `overwriteWith(imported)`가 정한다. 자연키로 찾아 있으면 덮어쓰고 없으면 넣는 흐름은 Service에 둔다.
- 기존 행은 행마다 조회하지 않고 출처별 `findAllBySource` 한 번(안주는 fetch join `findAll` 한 번)으로 불러와 Map에서 찾는다.
- 규칙표에 없는 값이 있는 행은 기본값으로 채우지 않고 적재에서 빼 리포트에 남긴다.

### 출처별 규칙

- BJCP 원본 JSON은 커밋하지 않는다. `scripts/import/fetch-bjcp.sh`가 beerjson/bjcp-json의 고정 커밋에서 받아 체크섬으로 검증한다.
- 국내 유통 맥주는 매핑표 `import/curated/beers.csv`로 적재한다. 제품마다 BJCP 스타일 id 하나를 가리키고, `ImportCuratedDrinkService`가 그 스타일의 기본 맛을 복사한 뒤 `(CURATED, external_id)`로 upsert한다. 제품별 맛 보정 열은 두지 않는다. 매핑한 스타일이 없으면 저장하지 않고 리포트에 남긴다.
- X-Wines(Slim) 원본 CSV는 `scripts/import/data/x-wines/`에 커밋하고 `import.x-wines.path`로 읽는다. 스타일은 `Type` 6종이고 `(X_WINES, type 슬러그)`로 식별하며 이름은 영문(`Red Wine` 등)이다. `ImportXWineService`가 한 트랜잭션에서 스타일을 먼저 적재한다.
- 와인 맛은 `Type`의 기본 맛에서 무게감을 `Body`로, 산미를 `Acidity`로 덮어쓴다. 값은 `wine-type.csv`, `wine-body.csv`, `wine-acidity.csv`에 둔다. 도수 25% 초과 행은 적재하지 않는다.
- 안주는 매핑표 `import/curated/anjus.csv`에 이름, `external_id`, `kfind_code`, 주재료, 부재료, 주재료 계열, 형태, 튀김 여부를 적는다. `external_id`는 사람이 정한 `anju-001` 같은 키이고, 한 번 정하면 바꾸거나 다시 쓰지 않는다. `kfind_code`는 적재에서만 쓰는 조회 키라 `anju` 테이블에 넣지 않는다.
- 안주의 단맛은 당류 구간, 무게감은 지방 구간으로 정한 뒤 형태와 튀김 여부로 보정한다. 산미와 쓴맛은 재료 규칙표(`ingredients.csv`)의 맛 태그 중 재료 전체에서 가장 큰 값을 쓴다. 탄산은 0이다. 알레르기는 주재료와 부재료 전체에서, 매운맛은 주재료에서만 계산한다.
- 안주의 표시명은 업체명과 제품명을 합치고, `uk_anju_name`을 유지한다.
- 적재가 소유한 K_FIND 안주 중 이번에 적재하지 않은 안주는 지운다. 피드백이 참조해 지울 수 없는 안주는 리포트에 남긴다. 지우기를 바로 반영한 뒤 저장한다.
- K-FIND 식품코드는 공공데이터포털 표준데이터(음식 15100070, 가공식품 15100066) CSV에서 `scripts/import/extract-kfind.py`로 고르고, 안주에 쓰는 행만 뽑은 `import/kfind/nutrition.csv`를 커밋한다.
- 식품코드는 매핑표의 요리와 같은 요리를 먼저 고른다. 출처의 정확도(분석 함량 우선, 프랜차이즈 제외)는 같은 요리 후보가 여럿일 때만 본다. 같은 요리가 없으면 주재료와 조리법이 같은 요리로 대신하고 결정 기록에 남긴다. 대신할 요리마저 맛이 달라지면 식품코드를 비워 적재에서 뺀다.
- 한 식품코드를 두 안주에 쓰지 않는다.
- 가공식품 안주의 알레르기는 운영 DB에 안주를 처음 적재하기 전에 HACCP 제품이미지 및 포장지표기정보(15033307)와 대조한다. 대조 결과는 `nutrition.csv`의 `raw_materials`와 같은 방식으로 적재에 더한다.

## 카탈로그 캐시

- 카탈로그 전체(`CatalogResult`)를 Redis 키 `catalog:v1` 하나에 JSON으로 캐시하고, 만료 시간은 24시간이다. `CatalogResult` 필드를 바꾸면 키의 버전을 올린다. 모르는 필드와 빠진 필드는 오류 없이 넘어가므로 옛 값이 조용히 잘못 읽힌다.
- 읽기는 Cache-Aside다. `CatalogQueryService.get`이 캐시에서 읽고, 없으면 `RefreshCatalogCacheUseCase`로 DB에서 읽어 채운다. 도수 상한은 `CatalogResult.filterByMaxAbv`로 메모리에서 적용하고, 상한별로 키를 나누지 않는다.
- 갱신은 Write-Through다. `ImportCatalogRunner`가 적재를 모두 마친 뒤 새 카탈로그로 덮어쓰고, 적재가 중간에 실패하면 `EvictCatalogCacheUseCase`로 키를 지운다.
- 캐시 인터페이스 `CatalogCache`는 `catalog.application.cache`에, 구현체 `CatalogCacheImpl`은 `catalog.infrastructure.redis`에 두고 `StringRedisTemplate`을 쓴다. Spring Cache와 Caffeine은 쓰지 않는다.
- Redis 장애와 역직렬화 실패는 구현체가 경고 로그만 남기고 DB 조회로 대체한다. 역직렬화에 실패한 값은 미스로 보고 DB에서 읽은 값으로 다시 덮어쓴다.
- 평점 집계(`PairingRatingSummary`)는 캐시하지 않는다. 피드백이 다음 추천에 바로 반영되어야 한다.

## 프로파일 설정

- 설정은 `application.yaml`과 `application-{local,dev,prod}.yaml`로 나눈다. `spring.profiles.default: local`이고, dev와 prod는 `SPRING_PROFILES_ACTIVE`로 반드시 지정한다.
- local의 datasource와 Redis 값은 compose를 끄거나 jar로 실행할 때를 위해 명시한다. `bootRun`과 `importCatalog`에서는 `spring-boot-docker-compose`의 연결이 우선한다.
- dev와 prod는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`로 주입한다. 6개 모두 필수이고, 비밀번호가 없는 Redis라도 `REDIS_PASSWORD`는 빈 값으로 넣는다.
- dev와 prod는 `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`로, local은 compose와 같은 `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`로 주입한다.
- `GATHERING_LINK_BASE_URL`은 local에만 기본값을 두고, dev와 prod는 기본값 없이 주입한다.
- dev와 prod는 `spring.docker.compose.enabled: false`로 compose 연결을 끈다.
- SQL 로그(`show-sql`, `format_sql`)는 local에서만 켠다. `refresh-token.cookie-secure`는 local에서만 기본값이 false이고, dev, prod, test에서는 true다.
- JWT, OAuth, CORS는 환경변수로 주입한다. 비밀값은 `${ENV_VAR}`, 만료 시간과 CORS는 `${ENV_VAR:기본값}`이다. `.env`는 Spring이 직접 읽지 않고 실행 환경에서 주입한다.

## 시간대와 Clock

- 서비스의 모든 시각은 한국 시간이다. `main`이 JVM 기본 시간대를 Asia/Seoul로 고정하고, 테스트 JVM도 `user.timezone`을 같게 둔다.
- 시각에 따라 동작이 달라지는 서비스는 `LocalDateTime.now()`를 직접 호출하지 않고 `Clock` 빈을 주입받는다.
