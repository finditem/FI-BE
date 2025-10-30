package com.fmi.domain.userblock.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.userblock.data.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {
    boolean existsByBlockerAndBlocked(User blocker, User blocked);
    Optional<BlockedUser> findByBlockerAndBlocked(User blocker, User blocked);
    List<BlockedUser> findAllByBlocker(User blocker);
}


