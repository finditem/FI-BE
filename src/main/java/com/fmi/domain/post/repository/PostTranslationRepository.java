package com.fmi.domain.post.repository;

import com.fmi.domain.Enum.LanguageCode;
import com.fmi.domain.post.data.PostTranslation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostTranslationRepository extends JpaRepository<PostTranslation, Long> {

    Optional<PostTranslation> findByPost_IdAndLanguageCode(Long postId, LanguageCode languageCode);

    List<PostTranslation> findAllByPost_Id(Long postId);
}
