# 테스트 메서드의 given-when-then 블록 주석과 메서드명 접두어를 검사한다(check-test-conventions.sh 참고).
#
# @Test/@ParameterizedTest/@RepeatedTest 다음에 나오는 "void 이름(" 줄부터 중괄호가 닫힐 때까지를 메서드 본문으로 본다.
# 중괄호는 문자열, 문자, text block("""), // 주석 밖에 있는 것만 센다. /* */ 블록 주석은 처리하지 않는다.
# 블록 주석은 "// given", "// when", "// then", "// when & then"이며 뒤에 ": 설명"을 붙일 수 있다.
#
# 위반은 "파일:줄: 메서드명: 사유"로 출력하고, 하나라도 있으면 종료 코드 1을 반환한다.
function reset_method() {
    in_method = 0; opened = 0
    given_count = 0; when_count = 0; then_count = 0; when_then_count = 0
    given_line = 0; when_line = 0; then_line = 0; when_then_line = 0
}

function report(reason) {
    printf "%s:%d: %s: %s\n", method_file, method_line, method_name, reason
    violations++
}

function check_method(   first_when) {
    if (method_name !~ /^should/) report("메서드명이 should로 시작하지 않는다")

    if (when_count + when_then_count == 0) { report("// when 또는 // when & then 블록이 없다"); return }
    if (when_count && when_then_count) { report("// when과 // when & then이 함께 있다"); return }
    if (when_count + when_then_count > 1) { report("when 블록이 두 번 이상 나온다"); return }
    if (given_count > 1) report("// given 블록이 두 번 이상 나온다")
    if (then_count > 1) report("// then 블록이 두 번 이상 나온다")

    first_when = when_count ? when_line : when_then_line
    if (given_count && given_line > first_when) report("// given이 when 블록보다 뒤에 있다")

    if (when_count) {
        if (then_count == 0) report("// when 뒤에 // then 블록이 없다")
        else if (then_line < when_line) report("// then이 // when보다 앞에 있다")
    } else if (then_count) {
        report("// when & then과 // then이 함께 있다")
    }
}

# text block 밖의 코드에서 문자열, 문자 리터럴, // 주석을 지운 뒤 중괄호만 남긴다.
function code_braces(line,   parts, n, i, seg, out) {
    n = split(line, parts, /"""/)
    out = ""
    for (i = 1; i <= n; i++) {
        if (i > 1) in_text_block = !in_text_block
        if (in_text_block) continue
        seg = parts[i]
        gsub(/\\\\/, "", seg)
        gsub(/\\"/, "", seg)
        gsub(/\\'/, "", seg)
        # '"'의 큰따옴표가 문자열 짝으로 잡히지 않도록 문자 리터럴을 먼저 지운다.
        gsub(/'[^']'/, "", seg)
        gsub(/"[^"]*"/, "", seg)
        sub(/\/\/.*/, "", seg)
        out = out seg
    }
    gsub(/[^{}]/, "", out)
    return out
}

FNR == 1 {
    if (in_method) check_method()
    reset_method(); pending_test = 0; depth = 0; in_text_block = 0
}

{
    was_in_text_block = in_text_block
    trimmed = $0
    sub(/^[[:space:]]+/, "", trimmed)
    sub(/[[:space:]]+$/, "", trimmed)

    if (!was_in_text_block) {
        # 같은 줄에 메서드 선언이 이어지는 "@Test void shouldX() {"도 잡도록 선언 검사를 따로 둔다.
        if (trimmed ~ /^@(Test|ParameterizedTest|RepeatedTest)([[:space:](]|$)/) pending_test = 1
        if (pending_test && match(trimmed, /void[[:space:]]+[A-Za-z0-9_]+[[:space:]]*\(/)) {
            method_name = substr(trimmed, RSTART, RLENGTH)
            sub(/^void[[:space:]]+/, "", method_name)
            sub(/[[:space:]]*\($/, "", method_name)
            method_file = FILENAME
            method_line = FNR
            pending_test = 0
            reset_method()
            in_method = 1; base_depth = depth
        }

        if (in_method) {
            if (trimmed ~ /^\/\/[[:space:]]*when[[:space:]]*&[[:space:]]*then([[:space:]]*:.*)?$/) {
                if (!when_then_count++) when_then_line = FNR
            } else if (trimmed ~ /^\/\/[[:space:]]*given([[:space:]]*:.*)?$/) {
                if (!given_count++) given_line = FNR
            } else if (trimmed ~ /^\/\/[[:space:]]*when([[:space:]]*:.*)?$/) {
                if (!when_count++) when_line = FNR
            } else if (trimmed ~ /^\/\/[[:space:]]*then([[:space:]]*:.*)?$/) {
                if (!then_count++) then_line = FNR
            }
        }
    }

    # 중괄호마다 판정해야 "void shouldX() {}"처럼 한 줄에서 열고 닫히는 메서드도 끝을 잡는다.
    braces = code_braces($0)
    for (i = 1; i <= length(braces); i++) {
        depth += (substr(braces, i, 1) == "{") ? 1 : -1
        if (!in_method) continue
        if (depth > base_depth) opened = 1
        else if (opened) { check_method(); reset_method() }
    }
}

END {
    if (in_method) check_method()
    exit violations > 0
}
