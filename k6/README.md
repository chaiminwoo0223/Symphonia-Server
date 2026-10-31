# 부하테스트 (K6)

pairing 도메인의 `GET /api/v1/pairings/recommendations`를 대상으로 한 K6 부하테스트 스크립트다. 도구 선정 근거와 1차 범위 결정은 `.claude/skills/testing/SKILL.md`의 "부하테스트" 절(이슈 #70)에 있다.

현재는 로컬과 dev 프로파일 환경에서 스크립트를 실행하고 콘솔 요약으로 결과를 확인하는 범위까지만 다룬다. GCP 실행 위치와 Grafana Alloy 연동은 인프라 협의 후 별도 이슈에서 다룬다.

## 사전 준비

- [K6 설치](https://grafana.com/docs/k6/latest/set-up/install-k6/) (`brew install k6`)
- Symphonia 서버 기동. `spring-boot-docker-compose`가 `compose.yaml`(PostgreSQL, Redis)을 자동으로 띄우므로 별도 `docker compose up`은 필요 없다. Gradle은 `.env`를 읽지 않는다. 먼저 `.env`를 셸 환경변수로 불러온다. 그렇지 않으면 `JWT_SECRET`이 치환되지 않아 `WeakKeyException`으로 기동에 실패한다.

  ```bash
  set -a; source .env; set +a
  ./gradlew bootRun
  ```

  서버가 기동하면 Flyway 마이그레이션 시드로 Drink·Anju·MusicMood 카탈로그가 채워진다. 실제 규모로 측정하려면 같은 셸에서 `./gradlew importCatalog`로 BJCP와 X-Wines 음료를 적재한다. `RECOMMEND`는 인증이 필요 없는 `permitAll` 엔드포인트라 별도 로그인 준비는 필요 없다.

## 실행

```bash
k6 run k6/recommend.js
```

서버가 기본 포트(8080)가 아니거나 dev 환경을 대상으로 하려면 `BASE_URL`을 넘긴다.

```bash
k6 run -e BASE_URL=https://dev.symphonia.example.com k6/recommend.js
```

## 결과 확인

K6는 실행이 끝나면 `http_req_duration`, `http_req_failed` 등 기본 지표와 `thresholds` 통과 여부를 콘솔에 요약해서 보여준다. 기준선을 파일로 남기려면 `--summary-export`를 추가한다.

```bash
k6 run --summary-export=k6/results/recommend-baseline.json k6/recommend.js
```

`k6/results/`는 실행 시점마다 값이 달라지는 산출물이라 저장소에 커밋하지 않는다.

## 시나리오

- `relationshipType`, `moodType`을 무작위로 섞는다.
- `attendeeConstraints`, `attendeeAllergies`는 0~2개를 무작위로 골라 섞는다.
- 요청 사이에 `sleep`(think-time)을 두지 않는다. 실제 사용자 트래픽보다 VU당 요청 빈도가 높게 나오지만, 엔드포인트가 낼 수 있는 최대 처리량 기준으로 기준선을 잡기 위한 의도다.
- `isAdultConfirmed`는 항상 `true`로 고정한다. 이 값이 없거나 `false`인 실패 케이스는 JUnit 테스트(`PairingControllerTest`)가 이미 검증하고 있어 부하테스트 범위에서는 다루지 않는다.

## Threshold

| 지표 | 기준 | 비고 |
|---|---|---|
| `http_req_failed` | `rate < 1%` | |
| `http_req_duration` | `p(95) < 300ms`, `p(99) < 800ms` | 첫 로컬 실행 결과를 보고 재조정한다 |
| `empty_recommendation_rate` | `rate < 5%` | 200 응답이면서 추천 결과가 비어 있는 비율(커스텀 지표) |

## 모임 참여와 분담금 조회 (`gathering.js`)

`POST /api/v1/participants`와 `GET /api/v1/shares`를 같은 모임에 동시에 요청한다(이슈 #125). 두 API는 모임을 읽을 때 참여자 전원의 이름과 휴대폰을 복호화한다. 참여자가 늘어나는 동안 응답 시간이 기준을 넘으면 `architecture` 스킬 `gathering.md`의 참여자 전체 복호화 결정을 재검토한다.

모임 열기에는 회원 JWT가 필요하다. 로그인은 소셜 인가 코드가 있어야 해서, setup에서 서버와 같은 `JWT_SECRET`으로 액세스 토큰을 직접 서명한다. 서버를 띄운 셸에서 `.env`를 불러왔다면 같은 셸에서 실행한다.

```bash
k6 run -e JWT_SECRET="$JWT_SECRET" k6/gathering.js
```

- setup은 모임을 열고 30명을 참여시킨다. 첫 참여자가 계좌를 등록하고 30명 전원의 정산 건을 기록한다.
- `join` 시나리오는 1분 동안 고른 간격으로 같은 모임에 50명을 더 참여시킨다. 분담금 조회가 모두 참여자가 늘어나는 동안 일어나게 하기 위해서다. 끝나면 참여자는 80명이다.
- `shares` 시나리오는 VU 20개로 1분 동안 결제자를 뺀 참여자의 링크 토큰으로 분담금을 조회한다.
- 실행할 때마다 새 모임을 연다. 모임은 생성 29일 뒤 파기 대상이 되므로(#128) 측정 데이터를 따로 지우지 않는다.

| 지표 | 기준 | 비고 |
|---|---|---|
| `http_req_failed` | 시나리오별 `rate < 1%` | |
| `http_req_duration{scenario:join}` | `p(95) < 500ms` | 참여는 암호화와 HMAC 계산이 더해진다 |
| `http_req_duration{scenario:shares}` | `p(95) < 300ms`, `p(99) < 800ms` | `recommend.js`와 같은 기준이다 |
| `checks{scenario:shares}` | `rate == 1` | 결제자 묶음이 하나가 아니면 측정을 실패로 본다 |
