package com.fmi.domain.place.web.dto.response;

import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.place.service.result.AdminPlaceListResult;
import com.fmi.domain.place.service.result.AdminPlaceResult;
import com.fmi.domain.place.web.AdminPlaceCursor;
import java.time.LocalDateTime;
import java.util.List;

public record DeletedPlacePageResponse(List<PlaceItem> content, boolean hasNext, AdminPlaceCursor nextCursor) {
    public static DeletedPlacePageResponse from(AdminPlaceListResult result) {
        List<PlaceItem> content = result.places().stream().map(PlaceItem::from).toList();
        AdminPlaceCursor cursor = null;
        if (result.hasNext()) {
            AdminPlaceResult last = result.places().get(result.places().size() - 1);
            cursor = new AdminPlaceCursor(last.deletedAt(), last.placeId());
        }
        return new DeletedPlacePageResponse(content, result.hasNext(), cursor);
    }

    public record PlaceItem(Long placeId, String name, String nameEn, PlaceType type, LocalDateTime deletedAt) {
        public static PlaceItem from(AdminPlaceResult place) {
            return new PlaceItem(place.placeId(), place.name(), place.nameEn(), place.type(), place.deletedAt());
        }
    }
}
