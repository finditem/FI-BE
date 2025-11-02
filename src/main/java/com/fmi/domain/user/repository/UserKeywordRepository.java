package com.fmi.domain.user.repository;

import com.fmi.domain.Enum.Type;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.user.data.UserKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserKeywordRepository extends JpaRepository<UserKeyword, Long> {
    List<UserKeyword> findAllByUser(User user);
    List<UserKeyword> findAllByCategory(Type category);
    Optional<UserKeyword> findByUserAndCategoryAndKeyword(User user, Type category, String keyword);
    boolean existsByUserAndCategoryAndKeyword(User user, Type category, String keyword);
    void deleteByUserAndCategoryAndKeyword(User user, Type category, String keyword);
}


