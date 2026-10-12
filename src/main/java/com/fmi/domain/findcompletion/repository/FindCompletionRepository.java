package com.fmi.domain.findcompletion.repository;

import com.fmi.domain.findcompletion.data.FindCompletion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindCompletionRepository extends JpaRepository<FindCompletion, Long> {

    Optional<FindCompletion> findBySourcePostId(Long sourcePostId);
}
