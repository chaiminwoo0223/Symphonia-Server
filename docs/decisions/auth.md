# auth 결정 기록

인증, 토큰, 보안 wiring의 근거와 이력을 둔다. 현재 규칙은 `architecture` 스킬이 단일 소스다.

## 보안 필터의 auth domain 참조

**(2026-10-29 결정, 이슈 #147)**: `global.security`의 인증 필터와 인증 실패 핸들러는 `auth`의 `domain` 타입을 참조할 수 있다. 필터는 인터페이스(`AccessTokenProvider`, `BlacklistAccessTokenRepository`)를, 핸들러는 에러 코드(`AuthErrorCode`)를 참조한다. 요청의 토큰을 검증해 인증 정보를 채우고 인증 실패를 응답하는 것은 보안 wiring 목적이기 때문이다. 구현체(`JwtAccessTokenProvider` 등)와 `infrastructure` 패키지는 참조하지 않는다. 토큰 Provider도 Repository처럼 `domain`에 인터페이스를 두고 `infrastructure`가 구현한다. gathering의 `GatheringTokenProvider`와 같은 구조다. 인터페이스가 기본 이름을 갖고, 구현체 이름에는 방식을 붙인다(`JwtAccessTokenProvider`, `UuidRefreshTokenProvider`).

## 로그아웃

**로그아웃 (2026-11-01 결정, 이슈 #156)**: 로그아웃은 이 기기의 세션만 끊고, 토큰이 없거나 만료돼도 204를 반환한다. 그래서 `LOGOUT`은 인증 없이 호출할 수 있고, 쿠키의 리프레시 토큰 하나를 `consume`으로 지운다. 액세스 토큰은 필터가 인증한 유효한 토큰일 때만 블랙리스트에 올린다. 이전에는 로그아웃에 인증이 필요해서, 액세스 토큰이 만료되면 401이 나고 리프레시 토큰이 남았다. 쿠키 `Path`도 재발급 엔드포인트라 로그아웃 요청에 쿠키가 실리지 않았다. 사용자가 로그아웃을 눌렀을 때 오류를 보여 주지 않고, 누른 기기만 끊는 것이 일반적인 기대이기 때문이다. 만료된 액세스 토큰을 로그아웃에서만 허용하는 안은 택하지 않았다. `AccessTokenProvider`에 만료를 무시하는 파싱이 생기고, 오래전에 유출된 토큰으로도 세션을 끊을 수 있기 때문이다. 회원 탈퇴는 모든 기기를 끊어야 하므로 `MemberDeletedEventListener`가 `ForceRevokeUseCase`로 회원의 리프레시 토큰을 모두 지우고 `LogoutUseCase`로 액세스 토큰을 블랙리스트에 올린다. 이미 발급된 옛 `Path` 쿠키는 처리하지 않았다. 결정 시점에 운영 배포가 없었기 때문이다.

## 401 응답 헤더

**401 응답 헤더 (2026-11-01 결정, 이슈 #156)**: `CustomAuthenticationEntryPoint`는 401 응답에 `WWW-Authenticate: Bearer realm="symphonia"` 헤더를 넣는다. RFC 9110과 RFC 6750이 401 응답에 인증 방식을 알리도록 요구하기 때문이다. RFC 6750은 `Bearer` 뒤에 인증 매개변수를 하나 이상 요구해서 `realm`을 붙인다. `GlobalExceptionHandler`가 응답하는 401(`REFRESH_TOKEN_NOT_FOUND`)에는 넣지 않는다. 재발급은 Bearer가 아니라 쿠키로 인증하고, 헤더를 넣으면 `common`이 인증 방식을 알아야 하기 때문이다.

## Rate Limiting 미적용

**Rate Limiting 미적용 (2026-10-08 결정, 이슈 #83)**: 앱 단 IP별 rate limit(`RateLimitFilter`)을 제거했다. signup·login은 일회용 OAuth 인가 코드를, refresh는 추측할 수 없는 UUID 리프레시 토큰을 받으므로 무차별 대입이 성립하지 않는다. 대량 요청 방어는 요청이 앱에 닿기 전에 막는 LB/WAF(예: Cloud Armor)가 맡는 편이 낫다. 필터를 유지하려면 프록시 뒤 IP 식별과 `INCR`/`EXPIRE` 원자화(Lua)를 함께 고쳐야 했는데, 그 비용에 비해 실익이 작아 택하지 않았다. 잘못된 인가 코드로 반복 호출해 카카오·구글 API 호출 한도를 소모시키는 공격이 관측되거나, 비밀번호 로그인처럼 대입 가능한 입력을 받는 엔드포인트가 생기면 재검토한다.

## 클라이언트 IP 식별

**클라이언트 IP 식별 (2026-10-08 결정, 이슈 #83)**: 클라이언트 IP는 감사 로그(`AuditLogRecorder`)의 `ip` 필드에만 쓰인다. `server.forward-headers-strategy: native`로 Tomcat `RemoteIpValve`를 켜고, `server.tomcat.remoteip.internal-proxies`(`TRUSTED_PROXIES` 환경변수, 기본값은 Spring Boot의 사설 IP 대역)에 든 프록시가 보낸 `X-Forwarded-For`만 반영한다. 그래서 IP는 어디서든 `HttpServletRequest.getRemoteAddr()`로 구하고, 별도 IP 추출 유틸을 두지 않는다. `framework` 전략(`ForwardedHeaderFilter`)은 헤더를 무조건 신뢰해 위조에 열려 있어 택하지 않았다. 신뢰 프록시 목록이 실제 LB 대역과 어긋나면 헤더가 무시되어 LB IP가 남을 뿐 위조에 열리지는 않으므로(#178에서 변경), 동작 검증은 테스트가 아니라 배포 시 `TRUSTED_PROXIES`를 넣고 감사 로그를 확인하는 것으로 한다. MockMvc는 Tomcat을 거치지 않아 이 동작을 검증할 수 없고, 실제 포트를 띄우는 테스트는 우리 코드가 아니라 Tomcat 구현을 검증하게 되어 두지 않았다. IP를 인가·차단 판단에 쓰기 시작하면 테스트 추가를 재검토한다.

**(2026-11-08 결정, 이슈 #178)**: #83 문단의 "위조에 열리지는 않는다"는 신뢰 프록시 목록이 실제 LB 대역보다 좁을 때만 맞다. 목록이 넓으면 그 대역 안의 다른 호스트가 `X-Forwarded-For`를 위조해 감사 로그에 원하는 IP를 남길 수 있다. 기본값인 사설 IP 대역 전체가 여기에 해당한다. 그래서 운영 환경의 `TRUSTED_PROXIES`에는 실제 LB 대역만 넣고, 앱 포트에는 LB만 접근할 수 있게 네트워크를 막는다. 감사 로그의 IP는 인가와 차단 판단에 쓰지 않으므로, 위조돼도 기록이 틀릴 뿐 접근 통제가 뚫리지는 않는다. 그래서 테스트 없이 배포 시 감사 로그로 확인하는 #83의 검증 방식은 유지한다.

