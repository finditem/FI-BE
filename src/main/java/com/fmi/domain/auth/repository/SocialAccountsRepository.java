package com.fmi.domain.auth.repository;

import com.fmi.domain.auth.data.SocialAccounts;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.Enum.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountsRepository extends JpaRepository<SocialAccounts, Long> {
    Optional<SocialAccounts> findByProviderAndProviderId(Provider provider, String providerId);
    Optional<SocialAccounts> findByUser(User user);
}


