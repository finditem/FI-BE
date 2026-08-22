package com.fmi.domain.userblock.repository;

import com.fmi.domain.user.data.User;
import com.fmi.domain.userblock.data.BlockedUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    Optional<BlockedUser> findByBlockerAndBlocked(User blocker, User blocked);

    List<BlockedUser> findAllByBlocker(User blocker);

    @Query("SELECT b FROM BlockedUser b JOIN FETCH b.blocked WHERE b.blocker = :blocker ORDER BY b.id DESC")
    Slice<BlockedUser> findByBlockerOrderByIdDesc(@Param("blocker") User blocker, Pageable pageable);

    @Query(
            "SELECT b FROM BlockedUser b JOIN FETCH b.blocked WHERE b.blocker = :blocker AND b.id < :cursor ORDER BY b.id DESC")
    Slice<BlockedUser> findByBlockerAndIdLessThanOrderByIdDesc(
            @Param("blocker") User blocker, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT b.blocked.id FROM BlockedUser b WHERE b.blocker.id = :blockerId")
    List<Long> findBlockedUserIdsByBlockerId(@Param("blockerId") Long blockerId);

    @Query("SELECT b.blocker.id FROM BlockedUser b WHERE b.blocked.id = :blockedId")
    List<Long> findBlockerIdsByBlockedId(@Param("blockedId") Long blockedId);
}
