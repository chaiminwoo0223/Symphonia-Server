# Java 파일을 읽어 주석과 문자열, 문자 리터럴, 텍스트 블록의 내용을 지운 줄을 `파일:줄:내용` 형식으로 출력한다.
# check-architecture.sh가 코드에 쓰인 표기만 검사하도록 grep 앞에 둔다.
# 실행: awk -f strip-java-comments-and-literals.awk <파일...>
#
# 여러 줄에 걸친 블록 주석과 텍스트 블록은 파일 안에서 상태를 이어 가며 지운다. 리터럴은 따옴표만 남긴다.

FNR == 1 { state = "code" }

{
    line = $0
    out = ""
    i = 1
    n = length(line)
    while (i <= n) {
        c = substr(line, i, 1)
        two = substr(line, i, 2)
        three = substr(line, i, 3)
        if (state == "block") {
            if (two == "*/") { state = "code"; out = out " "; i += 2 } else { i++ }
            continue
        }
        if (state == "text") {
            if (c == "\\") { i += 2; continue }
            if (three == "\"\"\"") { state = "code"; out = out "\"\"\""; i += 3 } else { i++ }
            continue
        }
        if (two == "//") { break }
        if (two == "/*") { state = "block"; i += 2; continue }
        if (three == "\"\"\"") { state = "text"; out = out "\"\"\""; i += 3; continue }
        if (c == "\"" || c == "'") {
            quote = c
            i++
            while (i <= n) {
                d = substr(line, i, 1)
                if (d == "\\") { i += 2; continue }
                i++
                if (d == quote) { break }
            }
            out = out quote quote
            continue
        }
        out = out c
        i++
    }
    print FILENAME ":" FNR ":" out
}
