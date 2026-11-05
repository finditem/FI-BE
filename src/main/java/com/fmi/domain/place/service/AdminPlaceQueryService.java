package com.fmi.domain.place.service;

import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.place.repository.AdminPlaceRepository;
import com.fmi.domain.place.service.result.AdminPlaceListResult;
import com.fmi.domain.place.service.result.AdminPlaceResult;
import com.fmi.domain.place.web.AdminPlaceCursor;
import com.fmi.domain.place.web.dto.request.AdminPlaceListRequest;
import com.fmi.global.data.EntityStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPlaceQueryService {
    private static final int PAGE_SIZE = 20;
    private final AdminPlaceRepository adminPlaceRepository;

    public AdminPlaceListResult getPlaces(AdminPlaceListRequest request, EntityStatus status) {
        PlaceType type = request.placeType();
        AdminPlaceCursor cursor = request.boundary();
        LocalDateTime cursorTime = null;
        Long cursorId = null;
        if (cursor != null) {
            cursorTime = cursor.time();
            cursorId = cursor.placeId();
        }
        List<AdminPlaceResult> candidates =
                adminPlaceRepository.findAll(status, type, request.keyword(), cursorTime, cursorId, PAGE_SIZE + 1);
        boolean hasNext = candidates.size() > PAGE_SIZE;
        int end = Math.min(candidates.size(), PAGE_SIZE);
        List<AdminPlaceResult> places = List.copyOf(candidates.subList(0, end));
        return new AdminPlaceListResult(places, hasNext);
    }
}
