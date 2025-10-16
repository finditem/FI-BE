package com.fmi.repository;

import com.fmi.domain.SocialAccounts;
import com.fmi.domain.Enum.Provider;
import com.fmi.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountsRepository extends JpaRepository<SocialAccounts, Long> {
    Optional<SocialAccounts> findByProviderAndProviderId(Provider provider, String providerId);
    Optional<SocialAccounts> findByUser(User user);
}


