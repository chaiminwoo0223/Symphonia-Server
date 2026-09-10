# 타입 선언(class/interface/enum/record) 여는 중괄호 바로 다음 줄의 빈 줄을 제거한다.
# Google Java Format(Spotless)은 사람이 넣은 단일 빈 줄은 지우지 않아 이 규칙은 걸러주지 못한다.
# src/main 전용이다. src/test는 testing 스킬이 문서화한 이중 @Nested 구조에서
# 여는 중괄호 다음 빈 줄을 의도적으로 사용하므로 대상에서 제외한다(post-edit-format.sh 참고).
#
# class/interface/enum/record 키워드가 나온 줄이 같은 줄에서 "{"로 끝나면 바로 다음 빈 줄(들)을 지운다.
# 선언이 여러 줄에 걸쳐 있으면(record의 다중 컴포넌트 등) 그 뒤로 "{"로 끝나는 줄이 나올 때까지
# 최대 5줄까지 기다렸다가 같은 방식으로 처리한다. 주석 줄(//, /*, *)은 키워드 검사에서 제외한다.
BEGIN { skip_blank = 0; pending = 0; pending_lines = 0 }
{
    line = $0
    trimmed = line
    sub(/^[[:space:]]*/, "", trimmed)

    if (skip_blank && line ~ /^[[:space:]]*$/) { next }
    skip_blank = 0

    if (pending) {
        pending_lines++
        if (line ~ /\{[[:space:]]*$/) {
            skip_blank = 1
            pending = 0
        } else if (pending_lines > 5) {
            pending = 0
        }
        print line
        next
    }

    is_comment = (trimmed ~ /^(\/\/|\*|\/\*)/)
    if (!is_comment && line ~ /(^|[[:space:]])(class|interface|enum|record)[[:space:]]/) {
        if (line ~ /\{[[:space:]]*$/) {
            skip_blank = 1
        } else {
            pending = 1
            pending_lines = 0
        }
    }
    print line
}
