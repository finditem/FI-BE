package com.fmi.domain.place.repository;

import com.fmi.domain.place.data.FavoritePlaceCandidate;
import java.time.LocalDateTime;
import java.util.List;

public interface PlaceFavoriteStateRepository {
    void save(Long placeId, Long userId, LocalDateTime favoritedAt);

    void cancel(Long placeId, Long userId, LocalDateTime canceledAt);

    List<FavoritePlaceCandidate> findFavorites(Long userId, LocalDateTime lastFavoriteUpdatedAt);
}
