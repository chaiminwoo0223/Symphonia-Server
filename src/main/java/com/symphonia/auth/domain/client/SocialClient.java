package com.symphonia.auth.domain.client;

import com.symphonia.auth.domain.identity.SocialIdentity;

public interface SocialClient {
    SocialIdentity authenticate(String code);
}
