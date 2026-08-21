package com.fmi.domain.ipblock.repository;

import com.fmi.domain.ipblock.data.IpBlacklist;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IpBlacklistRepository extends JpaRepository<IpBlacklist, Long> {
    boolean existsByIp(String ip);

    Optional<IpBlacklist> findByIp(String ip);
}
