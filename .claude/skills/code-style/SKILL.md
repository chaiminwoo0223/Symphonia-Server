---
name: code-style
description: Load when writing or formatting Java code. 포맷터(Spotless + Google Java Format), 자동 적용/검증 방식, 긴 주석 줄바꿈, 멤버 선언 순서.
---

> **Language**: All user-facing responses for this task MUST be written in Korean. (Code, identifiers, logs, and other technical artifacts are excluded.)

# Code Style

포맷터는 **Spotless + Google Java Format(AOSP 스타일)**으로 확정했다(2026-09-05). `build.gradle`에 설정되어 있다.

```groovy
plugins {
    id 'com.diffplug.spotless' version '8.10.1'
}

spotless {
    java {
        googleJavaFormat().aosp()
    }
}
```

## 원칙

- **직접 스타일을 맞추려 하지 않는다.** Google Java Format(AOSP 스타일)은 들여쓰기(4칸), 줄바꿈, import 순서, 중괄호 위치까지 전부 고정된 규칙으로 결정한다. 사람이 임의로 다르게 포맷하면 다음 `spotlessApply` 실행 때 되돌아간다.
- **자동 포맷을 신뢰한다.** `.claude/hooks/post-edit-format.sh`(PostToolUse)가 Java 파일을 편집할 때마다 자동으로 `spotlessApply`를 실행한다. 직접 `./gradlew spotlessApply`를 실행할 필요는 거의 없다.
- **`spotlessCheck`는 `check` 태스크에 연결되어 있다.** 즉 `./gradlew clean build`(`check-all.sh`, `pre-push-check.sh`가 실행하는 명령)에 포맷 검증이 자동으로 포함된다. 별도로 `spotlessCheck` 스텝을 추가할 필요가 없다.

## 타입 선언 뒤 빈 줄

`src/main`에서는 타입 선언(class, interface, enum, record)의 여는 중괄호 바로 다음 줄에 빈 줄을 두지 않는다. Google Java Format은 사람이 넣은 빈 줄 하나를 지우지 않아서 이 규칙을 잡지 못한다. 그래서 `post-edit-format.sh`가 `.claude/scripts/strip-type-open-blank-line.awk`로 이 빈 줄을 지운다. `src/test`는 대상에서 뺀다. `testing` 스킬의 이중 `@Nested` 구조가 이 위치의 빈 줄을 의도적으로 쓰기 때문이다.

## 긴 주석 줄바꿈

`//` 주석이 한 줄 100자를 넘을 것 같으면 포맷터에 맡기지 않고 문장 경계에서 직접 줄을 나눈다. 한 줄에는 완결된 문장을 하나 이상 담고, 문장 하나가 100자를 넘으면 쉼표나 절 경계에서 나눈다. Google Java Format은 100자를 넘는 `//` 주석을 글자 수 기준으로 다시 감싼다. 그래서 "거리 감점이"와 "됐다"처럼 문장 중간에서 잘린 줄이 생긴다. 포맷터가 정하지 않는 것이 아니라 의미를 모르고 정하는 영역이라, 위 "직접 스타일을 맞추려 하지 않는다" 원칙의 예외다.

**(2026-10-22 결정, 이슈 #106)**: #106에서 라거 골든 테스트의 주석 한 줄이 100자를 넘어 `spotlessApply`가 "거리 감점이 / 됐다"로 잘랐다. 문장 중간 줄바꿈은 읽는 흐름을 끊는 안티패턴이라 규칙으로 정했다. 포맷터 설정으로 주석 재배치를 끄는 안은 택하지 않았다. Google Java Format은 주석 감싸기를 끄는 옵션이 없고, 끄더라도 100자를 넘는 줄이 그대로 남기 때문이다. 결정론적 검사도 넣지 않았다. 잘린 줄인지 의도한 줄바꿈인지는 문장 구조를 봐야 알 수 있어 `code-reviewer` 체크리스트로 검토한다.

## 멤버 선언 순서

포맷터는 멤버의 순서를 바꾸지 않는다. 그래서 순서는 아래 규칙으로 맞춘다. record, class, enum 모두에 적용한다.

1. static 상수
2. 인스턴스 필드 (class만 해당)
3. 생성자 (record의 compact constructor 포함)
4. public static 팩토리 메서드 (`of`, `from`, `determined` 등)
5. public 인스턴스 메서드
6. public 중첩 타입 (record, enum, class)
7. private 메서드 (static 여부와 관계없이 호출 순서로 둔다)
8. private 중첩 타입 (record, enum, class)

순서는 static 여부가 아니라 역할과 공개 범위로 정한다. 팩토리는 생성자와 같은 생성 역할이라 생성자 바로 뒤에 두고, private 헬퍼는 static이어도 구현 세부라 공개 API 뒤에 둔다. public 멤버는 public끼리, private 멤버는 private끼리 묶는다. 파일을 위에서 아래로 읽을 때 공개 API가 한 덩어리로 먼저 보이고 구현 세부가 나중에 보여야 읽기 쉽기 때문이다. 상수와 필드는 private이어도 맨 위에 둔다. 객체가 무엇을 들고 있는지 먼저 보여야 메서드를 읽을 수 있기 때문이다. private 메서드는 클린코드의 step-down 규칙을 따른다. 위에서부터 처음 호출되는 순서로 두되, 호출한 메서드 바로 아래에 그 메서드가 호출하는 메서드를 둔다(깊이 우선). 중첩 타입은 같은 공개 범위의 메서드를 다 읽은 뒤 참고하는 보조 정의라 그 묶음의 맨 끝에 둔다.

**(2026-10-18 결정, 이슈 #107)**: 중첩 타입을 공개 범위에 따라 둘로 나눴다. public 중첩 타입(결과 DTO의 항목 record 등)은 public 메서드 뒤에, private 중첩 타입은 private 메서드 뒤에 둔다. #77 규칙대로 중첩 타입을 모두 맨 아래에 두면 `ImportAnjuResult`처럼 public 메서드, private 메서드, public record 순서가 되어 공개 API가 두 덩어리로 갈라지기 때문이다. 이 결정으로 `ImportAnjuResult`, `ImportResult`의 항목 record를 private 메서드 앞으로 옮겼다. 나머지 파일은 private 메서드가 없거나 중첩 타입이 private이라 이미 맞다.

**(2026-10-13 결정, 이슈 #77)**: record마다 중첩 타입 위치가 달라(`ImportResult`는 중간, `KakaoUserInfoResponse`는 맨 위) 순서 규칙을 정했다. static 멤버를 모두 위로 모으는 안은 private 헬퍼가 공개 API보다 먼저 나와 읽는 흐름이 끊기고 기존 코드 대부분을 바꿔야 해서 택하지 않았다. 검증은 `code-reviewer` 체크리스트로 한다. 호출 순서처럼 판단이 섞이는 규칙이라 결정론적 검사에 맞지 않기 때문이다. Checkstyle(`DeclarationOrder`, `InnerTypeLast`)은 상수, 생성자, 중첩 타입 위치만 잡고 팩토리와 private 메서드 순서는 잡지 못해 새 도구를 들일 만큼 이득이 크지 않았다. awk 스크립트는 Java 구조를 정규식으로 해석해야 해서 오탐이 많아 택하지 않았다. 리뷰에서 같은 위반이 반복해서 나오면 Checkstyle 도입을 재검토한다. 이번에는 record만 맞췄고, class와 enum은 수정하는 파일부터 리뷰에서 맞춘다.

## 중첩 타입 위치

중첩 타입을 바깥 타입 안에 둘지 최상위로 꺼낼지는 그 타입이 무엇에 속하는지로 정한다. 공개 범위는 누가 쓰는지로 따로 정한다.

- 바깥 타입 없이 의미가 없는 타입은 public이어도 바깥 타입 안에 둔다. 결과 DTO의 항목 record(`ImportAnjuResult.ExcludedItem`)가 여기에 해당한다. 다른 패키지에서 읽으면 public으로 둔다.
- 다른 클래스가 함께 쓰거나, 바깥 클래스와 다른 도메인 책임을 맡을 때만 최상위로 꺼낸다. `Pairing`과 `DrinkAnjuScores`가 함께 쓰는 `ScoredDrinkAnju`, 추천 정책과 별개로 대량의 쌍을 계산하는 `DrinkAnjuScores`와 `CandidateThresholds`가 여기에 해당한다.
- 바깥 클래스의 private 생성자를 써야 하는 타입은 바깥 클래스 안에 private으로 둔다. `Pairing.Combination`은 `Pairing`을 만드는 길을 `recommend` 하나로 묶어 두므로 꺼내지 않는다.
- 도메인 개념이 아닌 구현 세부는 쓰는 클래스 안에 private으로 둔다. 가변 누적기 `CandidateThresholds.TopScores`가 여기에 해당한다. 꺼내면 값 객체가 모이는 `domain/vo`에 가변 객체가 섞이고, 패키지 전체에 열린다.

**(2026-10-18 결정, 이슈 #107)**: `Pairing`의 후보 계산 클래스를 분리하면서 어떤 중첩 타입은 꺼내고 어떤 타입은 남길지 판단이 갈렸다. 그래서 소속과 공개 범위를 나눠 보는 기준을 정했다. public이면 무조건 최상위로 꺼내는 안은 택하지 않았다. 결과 DTO의 항목 record처럼 바깥 타입에 속한 타입이 흩어지고, `ImportResult.ExcludedItem`과 `ImportAnjuResult.ExcludedItem`처럼 이름이 겹쳐 출처를 이름에 붙여야 하기 때문이다. 쓰는 곳이 하나인 구현 세부를 범용 유틸로 미리 꺼내는 안도 YAGNI라 택하지 않았다. 두 번째로 쓰는 곳이 생기면 그때 꺼낸다.

## 명령어

```bash
./gradlew spotlessApply   # 포맷 자동 적용 (훅이 대신 실행해주므로 수동 실행은 드묾)
./gradlew spotlessCheck   # 포맷 위반 여부만 확인 (수정하지 않음)
```

## 이 스킬이 다루지 않는 것

네이밍 규칙, 계층별 타입 어휘, 클린코드 원칙(YAGNI, 단일 책임 등)은 이 스킬의 범위가 아니다. `architecture` 스킬과 `code-reviewer` 에이전트의 체크리스트를 참고한다. 이 스킬은 기계적으로 결정되는 포맷(공백, 줄바꿈, import 순서)과 포맷터가 정하지 않거나 의미를 모르고 정하는 영역(멤버 선언 순서, 긴 주석 줄바꿈)만 다룬다.
