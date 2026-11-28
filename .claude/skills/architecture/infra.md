# dev 인프라

`architecture` 스킬의 하위 문서다. 언제 읽는지는 `SKILL.md`의 하위 문서 표를 참고한다. 근거는 `docs/decisions/infra.md`에 있다.

- Terraform은 dev 환경만 다룬다. 전용 GCP 프로젝트를 사용하고, 실행 순서는 `infra/README.md`를 따른다.
- 모듈은 `persistent`, `app`, `loadgen`으로 나눈다. VM과 함께 지워도 되는 리소스만 `app`에 둔다.
- 앱, Redis, Caddy는 앱 VM의 Docker Compose로 실행하고, DB만 Cloud SQL로 분리한다.
- 외부에는 80, 443 포트만 연다. SSH는 IAP 대역만 허용한다.
- DB는 공인 IP를 쓰되 앱 VM의 고정 IP만 허용하고 `verify-ca`로 접속한다. 사설 IP 피어링은 만들지 않는다.
- 비밀값의 이름은 `infra/secrets.txt`에만 적는다. 값은 Secret Manager에 Terraform 밖에서 등록하고, 상태에 남기지 않는다.
- `GATHERING_MASTER_KEY`는 바꾸지 않는다.
- Redis는 swap을 쓰지 않게 하고 `noeviction`과 AOF를 유지한다. 앱에는 `MALLOC_ARENA_MAX=2`를 유지한다.
- 새 리소스는 `teardown.sh`로 삭제되게 하고, `check-leftovers.sh`에 확인 항목을 추가한다.
- 부하 테스트는 `loadgen` VM에서 사용자가 없는 시간에 실행하고, 끝나면 VM을 지운다.
