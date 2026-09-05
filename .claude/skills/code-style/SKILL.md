---
name: code-style
description: Load when writing or formatting Java code. 포맷터(Spotless + Google Java Format), 자동 적용/검증 방식.
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

## 명령어

```bash
./gradlew spotlessApply   # 포맷 자동 적용 (훅이 대신 실행해주므로 수동 실행은 드묾)
./gradlew spotlessCheck   # 포맷 위반 여부만 확인 (수정하지 않음)
```

## 이 스킬이 다루지 않는 것

네이밍 규칙, 계층별 타입 어휘, 클린코드 원칙(YAGNI, 단일 책임 등)은 이 스킬의 범위가 아니다. `architecture` 스킬과 `code-reviewer` 에이전트의 체크리스트를 참고한다. 이 스킬은 순수하게 기계적으로 결정되는 포맷(공백, 줄바꿈, import 순서)만 다룬다.
