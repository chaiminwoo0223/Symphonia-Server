# 부하테스트 (K6)

pairing 도메인의 `GET /api/v1/pairings/recommend`를 대상으로 한 K6 부하테스트 스크립트다. 도구 선정 근거와 1차 범위 결정은 `.claude/skills/testing/SKILL.md`의 "부하테스트" 절(이슈 #70)에 있다.

현재는 로컬과 dev 프로파일 환경에서 스크립트를 실행하고 콘솔 요약으로 결과를 확인하는 범위까지만 다룬다. GCP 실행 위치와 Grafana Alloy 연동은 인프라 협의 후 별도 이슈에서 다룬다.

## 사전 준비

- [K6 설치](https://grafana.com/docs/k6/latest/set-up/install-k6/) (`brew install k6`)
- Symphonia 서버 기동. `spring-boot-docker-compose`가 `compose.yaml`(PostgreSQL, Redis)을 자동으로 띄우므로 별도 `docker compose up` 없이 다음 명령이면 충분하다.

  ```bash
  ./gradlew bootRun
  ```

  서버가 기동하면 `data.sql` 시드로 Drink·Anju·MusicMood 카탈로그가 채워진다. `RECOMMEND`는 인증이 필요 없는 `permitAll` 엔드포인트라 별도 로그인 준비는 필요 없다.

## 실행

```bash
k6 run loadtest/recommend-load-test.js
```

서버가 기본 포트(8080)가 아니거나 dev 환경을 대상으로 하려면 `BASE_URL`을 넘긴다.

```bash
k6 run -e BASE_URL=https://dev.symphonia.example.com loadtest/recommend-load-test.js
```

## 결과 확인

K6는 실행이 끝나면 `http_req_duration`, `http_req_failed` 등 기본 지표와 `thresholds` 통과 여부를 콘솔에 요약해서 보여준다. 기준선을 파일로 남기려면 `--summary-export`를 추가한다.

```bash
k6 run --summary-export=loadtest/results/recommend-baseline.json loadtest/recommend-load-test.js
```

`loadtest/results/`는 실행 시점마다 값이 달라지는 산출물이라 저장소에 커밋하지 않는다.

## 시나리오

- `relationshipType`, `moodType`을 무작위로 섞는다.
- `attendeeConstraints`, `attendeeAllergies`는 0~2개를 무작위로 골라 섞는다.
- `isAdultConfirmed`는 항상 `true`로 고정한다. 이 값이 없거나 `false`인 실패 케이스는 JUnit 테스트(`PairingControllerTest`)가 이미 검증하고 있어 부하테스트 범위에서는 다루지 않는다.

## Threshold

| 지표 | 기준 | 비고 |
|---|---|---|
| `http_req_failed` | `rate < 1%` | |
| `http_req_duration` | `p(95) < 300ms`, `p(99) < 800ms` | 첫 로컬 실행 결과를 보고 재조정한다 |
| `empty_recommendation_rate` | `rate < 5%` | 200 응답이면서 추천 결과가 비어 있는 비율(커스텀 지표) |
