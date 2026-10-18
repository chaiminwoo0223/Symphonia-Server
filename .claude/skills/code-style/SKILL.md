---
name: code-style
description: Load when writing or formatting Java code. 포맷터(Spotless + Google Java Format), 자동 적용/검증 방식, 멤버 선언 순서.
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

## 명령어

```bash
./gradlew spotlessApply   # 포맷 자동 적용 (훅이 대신 실행해주므로 수동 실행은 드묾)
./gradlew spotlessCheck   # 포맷 위반 여부만 확인 (수정하지 않음)
```

## 이 스킬이 다루지 않는 것

네이밍 규칙, 계층별 타입 어휘, 클린코드 원칙(YAGNI, 단일 책임 등)은 이 스킬의 범위가 아니다. `architecture` 스킬과 `code-reviewer` 에이전트의 체크리스트를 참고한다. 이 스킬은 기계적으로 결정되는 포맷(공백, 줄바꿈, import 순서)과 포맷터가 정하지 않는 멤버 선언 순서만 다룬다.
