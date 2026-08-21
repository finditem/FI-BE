package com.fmi.domain.auth.repository;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.auth.data.SocialAccounts;
import com.fmi.domain.auth.data.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocialAccountsRepository extends JpaRepository<SocialAccounts, Long> {
    Optional<SocialAccounts> findByProviderAndProviderId(Provider provider, String providerId);

    @Query(
            "select sa from SocialAccounts sa join fetch sa.user where sa.provider = :provider and sa.providerId = :providerId")
    Optional<SocialAccounts> findByProviderAndProviderIdWithUser(
            @Param("provider") Provider provider, @Param("providerId") String providerId);

    Optional<SocialAccounts> findByUser(User user);

    boolean existsByUser(User user);
}
