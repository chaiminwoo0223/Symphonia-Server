#!/usr/bin/env bash
# 계층 금지 규칙 중 import와 선언만 보고 판정할 수 있는 항목을 검사한다(architecture 스킬 참고).
# 실행: ./.claude/scripts/check-architecture.sh
#
# 대상은 src/main의 Java 파일 전체다. 위반마다 위치와 고치는 방법을 출력하고 하나라도 있으면 실패한다.
# 완전 수식 이름 참조, 다른 도메인 enum의 메서드 호출, 절차적 도메인 로직처럼 판단이 필요한 규칙은
# code-reviewer가 검토한다. 주석 줄(//, /*, *로 시작하는 줄)은 검사하지 않는다.

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

MAIN="src/main/java/com/symphonia"
# 새 도메인이 검사에서 빠지지 않도록 공유 커널(common)과 기술 부트스트랩(global)을 뺀 패키지를 모두 도메인으로 본다.
DOMAINS="$(find "$MAIN" -mindepth 1 -maxdepth 1 -type d ! -name common ! -name global -exec basename {} \; | sort | tr '\n' ' ')"
violations=0

report() {
    local location="$1" problem="$2" fix="$3"
    echo "✗ $location: $problem"
    echo "  → $fix"
    violations=$((violations + 1))
}

# 일치하는 줄이 없어도 grep의 종료 코드 1로 스크립트가 멈추지 않게 한다.
search() {
    grep "$@" || true
}

# grep 결과(파일:줄:내용)에서 내용이 주석으로 시작하는 줄을 뺀다.
skip_comments() {
    search -vE '^[^:]+:[0-9]+:[[:space:]]*(//|/?\*)'
}

# grep 결과(파일:줄:내용)를 한 줄씩 받아 report로 넘긴다. 파이프로 넘기면 서브셸에서 세어 건수가 사라지므로
# `< <(...)`로 넘긴다.
report_matches() {
    local problem="$1" fix="$2" match
    while IFS= read -r match; do
        [[ -n "$match" ]] && report "$(cut -d: -f1,2 <<<"$match")" "$problem" "$fix"
    done
}

kotlin="$(find src -name '*.kt' | sort)"
while IFS= read -r file; do
    [[ -n "$file" ]] && report "$file" "Kotlin 파일이다" "Java로 다시 작성한다."
done <<<"$kotlin"

hexagonal="$(find "$MAIN" \( -name '*Adapter.java' -o -name '*Port.java' \) | sort)"
while IFS= read -r file; do
    [[ -n "$file" ]] && report "$file" "헥사고날 이름(*Adapter, *Port)이다" \
        "Repository 구현체는 *RepositoryImpl, 다른 도메인 인터페이스 구현체는 *ProviderImpl로 이름을 바꾼다."
done <<<"$hexagonal"

report_matches \
    "RuntimeException을 직접 던진다" \
    "도메인 *ErrorCode를 추가하고 BusinessException 하위 예외를 던진다(error-handling 스킬)." \
    < <(search -rn --include='*.java' -E 'new (java\.lang\.)?RuntimeException\(' "$MAIN" | skip_comments)

report_matches \
    "@Transactional을 직접 붙였다" \
    "애노테이션을 지우고 클래스에 @CommandService 또는 @QueryService를 붙인다. 성격이 둘이면 Service를 나눈다." \
    < <(search -rn --include='*.java' -E '@Transactional\b' "$MAIN" | search -v "^$MAIN/common/annotation/" | skip_comments)

for domain in $DOMAINS; do
    layer_dir="$MAIN/$domain/domain"
    [[ -d "$layer_dir" ]] || continue

    report_matches \
        "domain이 프레임워크를 import한다" \
        "프레임워크 의존 코드는 infrastructure나 application으로 옮긴다. 에러 코드의 HttpStatus만 허용한다." \
        < <(search -rn --include='*.java' -E '^import (static )?(org\.springframework\.|jakarta\.persistence\.)' "$layer_dir" | search -v ':import org\.springframework\.http\.HttpStatus;$')

    report_matches \
        "domain이 global이나 application, presentation, infrastructure를 import한다" \
        "domain은 다른 계층에 의존하지 않는다. 필요한 기능은 domain에 인터페이스를 두고 바깥 계층이 구현하게 한다." \
        < <(search -rn --include='*.java' -E '^import (static )?com\.symphonia\.(global\.|[a-z][a-z0-9_]*\.(application|presentation|infrastructure)\.)' "$layer_dir")
done

for domain in $DOMAINS; do
    layer_dir="$MAIN/$domain/application"
    [[ -d "$layer_dir" ]] || continue

    report_matches \
        "application이 global이나 presentation(*Request, *Response), infrastructure를 import한다" \
        "UseCase는 *Command, *Query, 원시값을 받는다. 변환은 Request.toCommand()와 Response.from(Result)에서 한다. 영속성과 외부 연동은 domain 인터페이스로 의존한다." \
        < <(search -rn --include='*.java' -E '^import (static )?com\.symphonia\.(global\.|[a-z][a-z0-9_]*\.(presentation|infrastructure)\.)' "$layer_dir")
done

# 다른 도메인에서는 application의 usecase, dto, event, provider와 domain의 enum만 참조한다.
for domain in $DOMAINS; do
    [[ -d "$MAIN/$domain" ]] || continue

    while IFS= read -r match; do
        [[ -n "$match" ]] || continue
        location="$(cut -d: -f1,2 <<<"$match")"
        fqcn="$(sed -E 's/.*import (static )?(com\.symphonia\.[A-Za-z0-9_.]+);.*/\2/' <<<"$match")"
        target="$(cut -d. -f3 <<<"$fqcn")"
        layer="$(cut -d. -f4 <<<"$fqcn")"
        [[ "$target" == "$domain" || "$target" == "common" || "$target" == "global" ]] && continue
        [[ " $DOMAINS " == *" $target "* ]] || continue

        case "$layer" in
            presentation)
                report "$location" "다른 도메인(${target})의 presentation을 import한다" \
                    "${target}의 *UseCase와 *Result를 의존한다. 경로 상수가 필요한 곳은 global뿐이다."
                ;;
            infrastructure)
                report "$location" "다른 도메인(${target})의 infrastructure를 import한다" \
                    "${target}의 *UseCase를 의존한다. 필요한 UseCase가 없으면 ${target}에 추가한다."
                ;;
            application)
                sub="$(cut -d. -f5 <<<"$fqcn")"
                if [[ "$sub" != "usecase" && "$sub" != "dto" && "$sub" != "event" && "$sub" != "provider" ]]; then
                    report "$location" "다른 도메인(${target})의 application.${sub} 구현을 import한다" \
                        "구현체 대신 ${target}의 *UseCase 인터페이스와 *Result를 의존한다."
                fi
                ;;
            domain)
                # 중첩 타입이나 static import면 마지막 마디가 멤버라서 타입 파일을 바로 찾지 못하면 한 마디를 뗀다.
                # 뗀 마디가 그 파일 안에 선언된 중첩 enum이면 enum으로 본다.
                path="src/main/java/$(tr . / <<<"$fqcn").java"
                member=""
                if [[ ! -f "$path" ]]; then
                    member="${fqcn##*.}"
                    path="src/main/java/$(tr . / <<<"${fqcn%.*}").java"
                fi
                type_name="$(basename "$path" .java)"
                is_enum=false
                if [[ -f "$path" ]]; then
                    grep -qE "^public enum $type_name\b" "$path" && is_enum=true
                    [[ -n "$member" ]] && grep -qE "^[[:space:]]+(public )?(static )?enum $member\b" "$path" && is_enum=true
                fi
                if [[ "$is_enum" == false ]]; then
                    report "$location" "다른 도메인(${target})의 domain에서 enum이 아닌 타입(${type_name})을 import한다" \
                        "${target}의 *UseCase가 돌려주는 *Result로 값을 받는다. 엔티티, 값 객체, Repository는 참조하지 않는다."
                fi
                ;;
        esac
    done < <(search -rn --include='*.java' -E '^import (static )?com\.symphonia\.' "$MAIN/$domain")
done

if [[ $violations -gt 0 ]]; then
    echo ""
    echo "✗ 계층 규칙 위반 ${violations}건. 위 → 안내대로 고친다(architecture 스킬)."
    exit 1
fi

echo "✓ 계층 규칙 검사를 통과했다."
