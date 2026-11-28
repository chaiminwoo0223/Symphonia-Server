# 한상 (Hansang)

"오늘 자리에는 어떤 술과 안주가 어울릴까?"에서 시작해 "누가 얼마를 보내면 될까?"까지, 술자리의 시작과 끝을 돕는 서비스입니다. 술과 안주를 자리의 분위기에 맞게 조합해 추천하고, 술자리 정산 기능을 함께 제공합니다. 회원은 Android 앱에서, 비회원은 웹에서 이용할 수 있습니다.

### 🍶 페어링 추천

- 함께하는 사람과의 관계, 분위기, 도수 상한, 알레르기를 반영해 술과 안주 조합을 추천 이유와 함께 알려 줍니다.
- 운전자, 임산부, 술을 마시지 않는 사람이 있는 자리에는 무알코올 선택지도 챙깁니다.
- 좋아요와 싫어요 피드백을 다음 추천에 반영하고, 로그인한 회원에게는 싫어요를 남긴 조합을 다시 추천하지 않습니다.

### 🧾 술자리 정산

- 모임장이 모임을 열면 참여자는 회원 가입 없이 QR과 구글 본인 확인으로 참여합니다.
- 차수별 결제 내역을 기록하면 분담금을 똑같이 나눠 계산하며, 참여자별 금액을 직접 지정할 수도 있습니다.
- 참여자는 개인 링크에서 낼 금액과 계산 근거를 확인해 "보냈어요"를 누르고, 결제자는 수금 화면에서 입금 현황을 확인합니다.
- 참여자의 이름과 휴대폰 번호는 모임별 키로 암호화해 저장하고, 보유 기간이 지나면 파기합니다.

### 📍 맛집 검색

- 지역명과 안주를 고르면 그 안주를 곁들일 가게를 찾아 줍니다.

서버는 도메인 주도 설계(DDD) 기반의 계층형 아키텍처를 단일 모듈, 도메인별 패키지 구조로 구성했습니다.

---

## 🛠 Tech Stack

| 분류 | 기술 스택 |
|------|-----------|
| **Application** | Java 25 / Spring Boot 4.0.10 / Gradle 9.5.1 |
| **Database & Cache** | PostgreSQL 17 (Hibernate JPA, Flyway) / Redis 8.8 |
| **Security** | Spring Security / JWT (jjwt 0.13.0) / 카카오·구글 로그인 |
| **External API** | NAVER API HUB 지역 검색 |
| **API Docs & Monitoring** | springdoc-openapi (Swagger UI) / Actuator, Micrometer Prometheus |
| **Testing & Lint** | JUnit 5 / Mockito / Testcontainers / K6 / Spotless (Google Java Format) |

---

## 📁 패키지 구조

```text
Symphonia-Server/
├── src/main/java/com/symphonia/
│   ├── member/        # 회원
│   ├── auth/          # 로그인, 토큰 재발급, 로그아웃
│   ├── catalog/       # 술, 안주와 맛·분류 값, 카탈로그 적재
│   ├── pairing/       # 자리에 맞는 술·안주 조합 추천, 피드백
│   ├── gathering/     # 모임, 참여, 정산
│   ├── place/         # 안주를 곁들일 맛집 검색
│   ├── common/        # 공통 예외, 응답 포맷, 공유 애노테이션
│   └── global/        # Security, JPA, Swagger 등 기술 설정
├── src/main/resources/
│   ├── db/migration/  # Flyway 마이그레이션
│   └── import/        # 카탈로그 적재 규칙표, 매핑표, 영양 데이터
├── docs/decisions/    # 설계 결정과 근거
├── k6/                # 부하테스트 스크립트
└── scripts/import/    # 카탈로그 원본 데이터 준비
```

도메인 패키지는 `domain`, `application`, `presentation`, `infrastructure` 계층으로 나눕니다.

---

## 💻 로컬 개발 환경 실행

JDK 25, Docker, Docker Compose 2.2.0 이상이 필요합니다.

### 1. 환경 변수 설정

프로젝트 루트에 `.env` 파일을 만듭니다.

| 변수 | 설명 |
|------|------|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | 로컬 PostgreSQL 설정 |
| `JWT_SECRET` | 액세스 토큰 서명 키 (32바이트 이상) |
| `GATHERING_MASTER_KEY` | 참여자 정보 암호화 키 (`openssl rand -base64 32`) |
| `KAKAO_APP_ID` | 카카오 로그인 앱 ID |
| `GOOGLE_CLIENT_ID` | 구글 로그인 웹 클라이언트 ID |
| `NAVER_LOCAL_SEARCH_CLIENT_ID`, `NAVER_LOCAL_SEARCH_CLIENT_SECRET` | NAVER API HUB 지역 검색 키 |

### 2. 애플리케이션 실행

PostgreSQL과 Redis 컨테이너는 실행할 때 `compose.yaml`로 자동 기동합니다.

```bash
set -a; source .env; set +a
./gradlew bootRun
```

### 3. 카탈로그 적재 (선택)

기본 시드 외의 술과 안주 데이터를 적재합니다. `curl`과 `jq`가 필요합니다. 2번과 같이 환경 변수를 불러온 셸에서 실행합니다.

```bash
./scripts/import/fetch-bjcp.sh
./gradlew importCatalog
```

### 4. 테스트

```bash
./gradlew test
```

---

## 📚 Wiki

서비스와 설계에 대한 상세 내용은 [**Wiki**](https://github.com/chaiminwoo0223/Symphonia-Server/wiki)를 참고하세요.

- [아키텍처](https://github.com/chaiminwoo0223/Symphonia-Server/wiki/아키텍처): 도메인 구성, 계층, 트랜잭션, 이벤트
- [API 공통 규칙](https://github.com/chaiminwoo0223/Symphonia-Server/wiki/API-공통-규칙): 인증 헤더, 응답 형식, 에러 코드, 엔드포인트
- [모임과 정산](https://github.com/chaiminwoo0223/Symphonia-Server/wiki/모임과-정산): 정산 흐름, 분담금 상태, 동시성
- [페어링 추천](https://github.com/chaiminwoo0223/Symphonia-Server/wiki/페어링-추천): 점수 계산과 피드백 집계
- [테스트와 성능](https://github.com/chaiminwoo0223/Symphonia-Server/wiki/테스트와-성능): 테스트 전략과 성능 개선 기록
