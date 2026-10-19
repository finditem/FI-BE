package com.fmi.domain.place.repository;

import java.time.LocalDateTime;

public interface PlaceFavoriteRepositoryCustom {
    long countFavorites(Long userId, LocalDateTime now);
}
