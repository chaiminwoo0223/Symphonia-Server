package com.symphonia.member.domain.policy;

import com.symphonia.member.domain.exception.MemberAlreadyExistsException;

public enum MemberPolicy {
    ;

    // member.nickname 컬럼은 255자이지만, 화면에 그대로 보여 주는 값이라 더 짧게 제한한다.
    // UpdateMemberRequest.nickname의 @Size(max)는 리터럴이라 이 값을 바꿀 때 함께 바꾼다.
    public static final int MAX_NICKNAME_LENGTH = 20;

    public static void validateNotDuplicated(boolean exists) {
        if (exists) {
            throw new MemberAlreadyExistsException();
        }
    }

    // 소셜 닉네임은 우리가 정한 길이를 지키지 않으므로 가입을 거절하지 않고 잘라서 저장한다.
    // 이모지 같은 서로게이트 쌍이 반으로 잘리지 않게 하고, 길이는 @Size와 같은 UTF-16 단위로 센다.
    public static String truncateNickname(String nickname) {
        if (nickname.length() <= MAX_NICKNAME_LENGTH) {
            return nickname;
        }
        int end = MAX_NICKNAME_LENGTH;
        if (Character.isHighSurrogate(nickname.charAt(end - 1))) {
            end--;
        }

        return nickname.substring(0, end);
    }
}
