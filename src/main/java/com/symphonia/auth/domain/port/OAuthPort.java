package com.symphonia.auth.domain.port;

import com.symphonia.auth.domain.identity.SocialIdentity;

public interface OAuthPort {
    SocialIdentity authenticate(String provider, String code);
}