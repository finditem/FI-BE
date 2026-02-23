package com.fmi.domain.userblock.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.userblock.data.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {
    boolean existsByBlockerAndBlocked(User blocker, User blocked);
    Optional<BlockedUser> findByBlockerAndBlocked(User blocker, User blocked);
    List<BlockedUser> findAllByBlocker(User blocker);

    @Query("SELECT b.blocked.id FROM BlockedUser b WHERE b.blocker.id = :blockerId")
    List<Long> findBlockedUserIdsByBlockerId(@Param("blockerId") Long blockerId);

    @Query("SELECT b.blocker.id FROM BlockedUser b WHERE b.blocked.id = :blockedId")
    List<Long> findBlockerIdsByBlockedId(@Param("blockedId") Long blockedId);
}


