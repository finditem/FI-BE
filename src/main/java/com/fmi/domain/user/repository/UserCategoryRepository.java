package com.fmi.domain.user.repository;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.data.UserCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCategoryRepository extends JpaRepository<UserCategory, Long> {
    List<UserCategory> findAllByUser(User user);

    List<UserCategory> findAllByCategory(Category category);

    boolean existsByUserAndCategory(User user, Category category);

    void deleteByUserAndCategory(User user, Category category);
}
