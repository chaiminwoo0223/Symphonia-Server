# dev 인프라

`mariajou.duckdns.org`로 서비스하는 dev 환경의 Terraform 코드와 운영 스크립트다. prod에는 적용하지 않는다.

## 구성

| 모듈 | 리소스 | 수명 |
|---|---|---|
| `persistent` | API, VPC, IAP SSH 방화벽, 고정 IP, Cloud SQL, Artifact Registry, Secret Manager | 완전 철거 때만 삭제 |
| `app` | 서비스 계정, 80·443 방화벽, 앱 VM(e2-small, swap 4GB) | `down.sh`로 삭제 |
| `loadgen` | k6 VM | 테스트할 때만 생성 |

앱 VM은 `deploy/dev/compose.yaml`로 앱, Redis, Caddy를 실행한다.

## 사전 준비

1. dev 전용 GCP 프로젝트를 만들고 결제를 연결한다.
2. Terraform 1.11 이상, gcloud, Docker, jq를 설치한다.
3. `gcloud auth login`과 `gcloud auth application-default login`을 실행한다.
4. 카카오 앱 ID, 구글 웹 클라이언트 ID, NAVER API HUB 키를 준비한다.
5. `export PROJECT_ID=<프로젝트 ID>`를 실행한다. 모든 스크립트가 이 값을 요구한다.

## 처음 배포

```bash
./infra/scripts/up.sh              # 계획을 확인하고 yes를 입력한다
# DuckDNS의 IP를 출력된 고정 IP로 바꾼다
./infra/scripts/deploy.sh          # 커밋한 상태에서 실행한다
./infra/scripts/import-catalog.sh
```

- `up.sh`는 값이 없는 비밀값만 등록한다. `GATHERING_MASTER_KEY`는 안내에 따라 비밀번호 관리자에도 보관한다.
- 반복해서 다시 만들며 검증할 때는 `ACME_CA=https://acme-staging-v02.api.letsencrypt.org/directory`를 붙여 `deploy.sh`를 실행한다. 같은 도메인의 인증서는 발급 횟수에 제한이 있다.

## 내리기

| 명령 | 하는 일 | 다시 올리기 |
|---|---|---|
| `pause.sh` | VM과 DB 중지. 보관 비용은 계속 발생 | `resume.sh` |
| `down.sh` | `loadgen`과 `app` 삭제. DB, IP, 이미지, 비밀값 유지 | `up.sh`, `deploy.sh` |
| `teardown.sh` | 전부 삭제하고 상태 버킷까지 제거 | 처음 배포부터 |

`teardown.sh`는 DB 덤프 여부를 묻고, 마지막에 `check-leftovers.sh`로 남은 리소스를 확인한다. GCP 감사 로그, 결제 내역, 인증서 투명성 로그는 바로 지울 수 없다.

## 그 밖의 작업

- DB 덤프: `dump-db.sh`는 `$DUMP_DIR`(기본값 홈 디렉터리)에 저장한다.
- 부하 생성기: `loadgen.sh up`과 `loadgen.sh down`을 쓴다. dev는 웹이 함께 쓰므로 사용자가 없는 시간에 테스트한다.
- VM 접속: `gcloud compute ssh mariajou-dev-app --tunnel-through-iap --zone asia-northeast3-a --project "$PROJECT_ID"`
- 사양 변경: `app`의 `machine_type`, `persistent`의 `db_tier`를 `TF_VAR_machine_type`, `TF_VAR_db_tier`로 바꾼다. DB 사양을 바꾸면 `DB_POOL_SIZE`를 함께 확인한다.
