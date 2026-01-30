package com.fmi.domain.ipblock.repository;

import com.fmi.domain.ipblock.data.IpBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IpBlacklistRepository extends JpaRepository<IpBlacklist, Long> {
    boolean existsByIp(String ip);
    Optional<IpBlacklist> findByIp(String ip);
}
