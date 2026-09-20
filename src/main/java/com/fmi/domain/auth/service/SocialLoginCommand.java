package com.fmi.domain.auth.service;

import com.fmi.domain.Enum.Provider;

public record SocialLoginCommand(Provider provider, String providerId, String nickname, String profileImageUrl) {}
