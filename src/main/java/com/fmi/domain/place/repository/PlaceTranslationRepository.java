package com.fmi.domain.place.repository;

import com.fmi.domain.Enum.LanguageCode;
import com.fmi.domain.place.data.PlaceTranslation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceTranslationRepository extends JpaRepository<PlaceTranslation, Long> {
    Optional<PlaceTranslation> findByPlace_IdAndLanguageCode(Long placeId, LanguageCode languageCode);

    List<PlaceTranslation> findAllByPlace_IdInAndLanguageCode(Collection<Long> placeIds, LanguageCode languageCode);
}
