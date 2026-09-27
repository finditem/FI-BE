package com.fmi.domain.auth.data;

import com.fmi.domain.Enum.Provider;

public record SocialLoginCommand(Provider provider, String providerId, String nickname, String profileImageUrl) {}
