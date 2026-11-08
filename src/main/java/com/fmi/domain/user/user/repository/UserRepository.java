package com.fmi.domain.user.user.repository;

import com.fmi.domain.user.user.data.User;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findActiveByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailIncludingDeletedForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    // deletedAt이 null인 활성 사용자만 조회
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    @Query(
            "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);

    @Query(
            "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.nickname = :nickname AND u.deletedAt IS NULL")
    boolean existsByNickname(@Param("nickname") String nickname);

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
    @Query(
            "SELECT u FROM User u WHERE u.temporaryPasswordExpiresAt IS NOT NULL AND u.temporaryPasswordExpiresAt < :now")
    List<User> findUsersWithExpiredTemporaryPassword(@Param("now") LocalDateTime now);

    Optional<User> findByNickname(String nickname);
}
