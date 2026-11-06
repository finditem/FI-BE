package com.fmi.domain.user.user.repository;

import com.fmi.domain.user.user.data.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends org.springframework.data.repository.Repository<User, Long> {

    // 탈퇴한 사용자 조회 (관리자용)
    @Query(
            value = "SELECT * FROM users u WHERE u.deleted_at IS NOT NULL "
                    + "AND (:reason IS NULL OR FIND_IN_SET(:reason, u.withdrawal_reason) > 0)",
            countQuery = "SELECT COUNT(*) FROM users u WHERE u.deleted_at IS NOT NULL "
                    + "AND (:reason IS NULL OR FIND_IN_SET(:reason, u.withdrawal_reason) > 0)",
            nativeQuery = true)
    Page<User> findDeletedUsers(@Param("reason") String reason, Pageable pageable);

    // 탈퇴한 사용자 커서 기반 조회 (관리자용)
    @Query(
            value = "SELECT * FROM users u WHERE u.deleted_at IS NOT NULL "
                    + "AND (:reason IS NULL OR FIND_IN_SET(:reason, u.withdrawal_reason) > 0) "
                    + "AND (:cursor IS NULL OR u.id < :cursor) "
                    + "ORDER BY u.id DESC LIMIT :limit",
            nativeQuery = true)
    List<User> findDeletedUsersCursor(
            @Param("reason") String reason, @Param("cursor") Long cursor, @Param("limit") int limit);

    @Query(
            value = "SELECT * FROM users u WHERE u.deleted_at IS NOT NULL "
                    + "AND (:reason IS NULL OR FIND_IN_SET(:reason, u.withdrawal_reason) > 0) "
                    + "AND (:keyword IS NULL OR u.email LIKE :keyword OR u.withdrawal_reason LIKE :keyword OR u.withdrawal_other_reason LIKE :keyword) "
                    + "AND (:cursor IS NULL OR u.id < :cursor) "
                    + "ORDER BY u.id DESC LIMIT :limit",
            nativeQuery = true)
    List<User> findDeletedUsersCursorWithKeyword(
            @Param("reason") String reason,
            @Param("keyword") String keyword,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);
}
