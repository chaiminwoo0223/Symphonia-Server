package com.symphonia.member.helper;

import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberFixture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberHelper {
    private final MemberRepository memberRepository;

    public Member saveActive(MemberFixture fixture) {
        return memberRepository.save(fixture.toActive());
    }

    public Member saveWithdrawn(MemberFixture fixture) {
        return memberRepository.save(fixture.toWithdrawn());
    }
}
