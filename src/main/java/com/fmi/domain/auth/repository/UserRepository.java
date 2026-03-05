package com.fmi.domain.auth.repository;

import com.fmi.domain.auth.data.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // deletedAt이 null인 활성 사용자만 조회
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmail(@Param("email") String email);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.nickname = :nickname AND u.deletedAt IS NULL")
    boolean existsByNickname(@Param("nickname") String nickname);
    
    // 일주일(7일) 이내 탈퇴한 이메일 체크 (재가입 방지용)
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.deletedAt IS NOT NULL AND u.deletedAt >= :oneWeekAgo")
    boolean existsRecentlyDeletedByEmail(@Param("email") String email, @Param("oneWeekAgo") LocalDateTime oneWeekAgo);
    
    // 30일 이상 된 삭제된 사용자 조회 (하드 삭제 스케줄러용)
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL AND u.deletedAt < :thirtyDaysAgo")
    List<User> findUsersDeletedBefore(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
    
    // 활성 사용자만 조회 (deletedAt이 null인 사용자)
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    List<User> findAllActiveUsers();

    // 활성 사용자 페이징 조회
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    Page<User> findAllActiveUsers(Pageable pageable);
    
    // 활성 사용자 ID로 조회 (deletedAt이 null인 사용자만)
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findActiveById(@Param("id") Long id);
    
    // 만료된 임시 비밀번호가 있는 사용자 조회 (정리 스케줄러용)
    @Query("SELECT u FROM User u WHERE u.temporaryPasswordExpiresAt IS NOT NULL AND u.temporaryPasswordExpiresAt < :now")
    List<User> findUsersWithExpiredTemporaryPassword(@Param("now") LocalDateTime now);

    // 탈퇴한 사용자 조회 (관리자용)
    @Query(value = "SELECT * FROM users u WHERE u.deleted_at IS NOT NULL " +
           "AND (:reason IS NULL OR FIND_IN_SET(:reason, u.withdrawal_reason) > 0)",
           countQuery = "SELECT COUNT(*) FROM users u WHERE u.deleted_at IS NOT NULL " +
           "AND (:reason IS NULL OR FIND_IN_SET(:reason, u.withdrawal_reason) > 0)",
           nativeQuery = true)
    Page<User> findDeletedUsers(@Param("reason") String reason, Pageable pageable);

    Optional<User> findByNickname(String nickname);

    // 탈퇴한 사용자 커서 기반 조회 (관리자용)
    @Query(value = "SELECT * FROM users u WHERE u.deleted_at IS NOT NULL " +
           "AND (:reason IS NULL OR FIND_IN_SET(:reason, u.withdrawal_reason) > 0) " +
           "AND (:cursor IS NULL OR u.id < :cursor) " +
           "ORDER BY u.id DESC LIMIT :limit",
           nativeQuery = true)
    List<User> findDeletedUsersCursor(@Param("reason") String reason,
                                      @Param("cursor") Long cursor,
                                      @Param("limit") int limit);

    @Query(value = "SELECT * FROM users u WHERE u.deleted_at IS NOT NULL " +
           "AND (:reason IS NULL OR FIND_IN_SET(:reason, u.withdrawal_reason) > 0) " +
           "AND (:keyword IS NULL OR u.email LIKE :keyword OR u.withdrawal_reason LIKE :keyword OR u.withdrawal_other_reason LIKE :keyword) " +
           "AND (:cursor IS NULL OR u.id < :cursor) " +
           "ORDER BY u.id DESC LIMIT :limit",
           nativeQuery = true)
    List<User> findDeletedUsersCursorWithKeyword(@Param("reason") String reason,
                                                  @Param("keyword") String keyword,
                                                  @Param("cursor") Long cursor,
                                                  @Param("limit") int limit);
}


