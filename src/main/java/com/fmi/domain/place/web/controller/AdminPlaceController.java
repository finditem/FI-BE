package com.fmi.domain.place.web.controller;

import com.fmi.domain.place.service.AdminPlaceQueryService;
import com.fmi.domain.place.service.PlaceService;
import com.fmi.domain.place.service.result.AdminPlaceListResult;
import com.fmi.domain.place.web.dto.request.AdminPlaceListRequest;
import com.fmi.domain.place.web.dto.request.PlaceUpsertRequest;
import com.fmi.domain.place.web.dto.response.AdminPlacePageResponse;
import com.fmi.domain.place.web.dto.response.DeletedPlacePageResponse;
import com.fmi.domain.place.web.dto.response.PlaceIdResponse;
import com.fmi.domain.place.web.dto.response.PlaceManagementResponse;
import com.fmi.domain.place.web.swagger.AdminPlaceSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.data.EntityStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/places")
@RequiredArgsConstructor
public class AdminPlaceController implements AdminPlaceSwagger {

    private final PlaceService placeService;
    private final AdminPlaceQueryService adminPlaceQueryService;

    @Override
    @GetMapping
    public ApiResponse<AdminPlacePageResponse> list(@ParameterObject @ModelAttribute AdminPlaceListRequest request) {
        AdminPlaceListResult result = adminPlaceQueryService.getPlaces(request, EntityStatus.ACTIVE);
        AdminPlacePageResponse response = AdminPlacePageResponse.from(result);
        return ApiResponse.onSuccess(response);
    }

    @Override
    @GetMapping("/deleted")
    public ApiResponse<DeletedPlacePageResponse> listDeleted(
            @ParameterObject @ModelAttribute AdminPlaceListRequest request) {
        AdminPlaceListResult result = adminPlaceQueryService.getPlaces(request, EntityStatus.DELETED);
        DeletedPlacePageResponse response = DeletedPlacePageResponse.from(result);
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<PlaceManagementResponse>> create(
            @Valid @RequestPart("request") PlaceUpsertRequest request,
            @RequestPart("thumbnail") MultipartFile thumbnail) {
        Long placeId = placeService.create(request.toCommand(), thumbnail);
        PlaceManagementResponse response = PlaceManagementResponse.from(placeService.getManagementDetail(placeId));
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Override
    @GetMapping("/{placeId}")
    public ApiResponse<PlaceManagementResponse> get(@PathVariable Long placeId) {
        return ApiResponse.onSuccess(PlaceManagementResponse.from(placeService.getManagementDetail(placeId)));
    }

    @Override
    @PutMapping("/{placeId}")
    public ApiResponse<PlaceManagementResponse> update(
            @PathVariable Long placeId,
            @Valid @RequestPart("request") PlaceUpsertRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
        placeService.update(placeId, request.toCommand(), thumbnail);
        return ApiResponse.onSuccess(PlaceManagementResponse.from(placeService.getManagementDetail(placeId)));
    }

    @Override
    @DeleteMapping("/{placeId}")
    public ApiResponse<PlaceIdResponse> delete(@PathVariable Long placeId) {
        placeService.delete(placeId);
        return ApiResponse.onSuccess(new PlaceIdResponse(placeId));
    }

    @Override
    @PatchMapping("/{placeId}/restore")
    public ApiResponse<PlaceIdResponse> restore(@PathVariable Long placeId) {
        placeService.restore(placeId);
        return ApiResponse.onSuccess(new PlaceIdResponse(placeId));
    }
}
