# Symphonia (심포니아) — 백엔드

술과 안주를 자리의 분위기에 맞게 추천하고, 술자리 정산을 돕는 서비스의 백엔드 API 서버입니다. (회원용 Android 앱과 비회원용 웹 클라이언트 대상)

도메인 주도 설계(DDD) 기반의 계층형 아키텍처를 단일 모듈, 도메인별 패키지 구조로 구성했습니다.

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
│   └── import/        # 카탈로그 적재 규칙표
├── docs/decisions/    # 설계 결정과 근거
├── k6/                # 부하테스트 스크립트
└── scripts/import/    # 카탈로그 원본 데이터 준비
```

도메인 패키지는 `domain`, `application`, `presentation`, `infrastructure` 계층으로 나눕니다.

---

## 💻 로컬 개발 환경 실행

JDK 25와 Docker가 필요합니다.

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

- 프로파일을 지정하지 않으면 `local`로 실행합니다.
- Swagger UI: http://localhost:8080/swagger-ui/index.html

### 3. 카탈로그 적재 (선택)

기본 시드 외의 술과 안주 데이터를 적재합니다. 2번과 같이 환경 변수를 불러온 셸에서 실행합니다.

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
