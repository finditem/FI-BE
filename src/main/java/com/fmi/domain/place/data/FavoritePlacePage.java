package com.fmi.domain.place.data;

import java.time.LocalDateTime;
import java.util.List;

public record FavoritePlacePage(
        List<PlaceSummary> places, long totalCount, boolean hasNext, LocalDateTime nextFavoriteUpdatedAt) {}
