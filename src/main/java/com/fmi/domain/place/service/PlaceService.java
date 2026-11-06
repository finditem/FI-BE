package com.fmi.domain.place.service;

import com.fmi.domain.place.data.Place;
import com.fmi.domain.place.data.PlaceDailySchedule;
import com.fmi.domain.place.data.PlaceManagementDetail;
import com.fmi.domain.place.data.PlaceOperationPeriod;
import com.fmi.domain.place.data.PlaceTranslation;
import com.fmi.domain.place.data.PlaceUpsertCommand;
import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.place.exception.PlaceErrorStatus;
import com.fmi.domain.place.repository.PlaceRepository;
import com.fmi.domain.place.repository.PlaceTranslationRepository;
import com.fmi.domain.place.service.internal.PlaceBusinessHourUpdater;
import com.fmi.domain.place.service.internal.PlaceValidator;
import com.fmi.domain.place.service.internal.PopupClosingAtCalculator;
import com.fmi.domain.user.user.data.enums.LanguageCode;
import com.fmi.external.storage.S3Service;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.dto.UploadedImage;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;

    private final PlaceTranslationRepository placeTranslationRepository;

    private final PlaceValidator placeValidator;

    private final PlaceBusinessHourUpdater placeBusinessHourUpdater;

    private final PopupClosingAtCalculator popupClosingAtCalculator;

    private final S3Service s3Service;

    private final Clock clock;

    @Transactional
    public Long create(PlaceUpsertCommand command, MultipartFile thumbnail) {
        PlaceOperationPeriod operationPeriod = command.type() == PlaceType.POPUP
                ? PlaceOperationPeriod.builder()
                        .startDate(command.operationStartDate())
                        .endDate(command.operationEndDate())
                        .build()
                : null;
        placeValidator.validateTranslations(command);
        placeValidator.validate(command.type(), operationPeriod, command.dailySchedules());
        UploadedImage uploadedThumbnail =
                s3Service.uploadWithThumbnail(List.of(thumbnail)).get(0);

        Place place = Place.builder()
                .name(command.name())
                .address(command.address())
                .latitude(command.latitude())
                .longitude(command.longitude())
                .station(command.station())
                .stationDistanceMeters(command.stationDistanceMeters())
                .type(command.type())
                .thumbnailUrl(uploadedThumbnail.thumbnailUrl())
                .operationPeriod(operationPeriod)
                .build();
        placeBusinessHourUpdater.update(place, command.dailySchedules(), LocalDateTime.now(clock));
        if (place.getType() == PlaceType.POPUP) {
            List<PlaceDailySchedule> dailySchedules = place.dailySchedules();
            LocalDateTime closingAt = popupClosingAtCalculator.calculate(operationPeriod, dailySchedules);
            operationPeriod.scheduleClosingAt(closingAt);
        }
        placeRepository.save(place);
        if (command.nameEn() != null || command.addressEn() != null || command.stationEn() != null) {
            PlaceTranslation translation = PlaceTranslation.create(
                    place, LanguageCode.EN, command.nameEn(), command.addressEn(), command.stationEn());
            placeTranslationRepository.save(translation);
        }
        return place.getId();
    }

    public PlaceManagementDetail getManagementDetail(Long placeId) {
        Place place =
                placeRepository.findById(placeId).orElseThrow(() -> new GeneralException(PlaceErrorStatus.NOT_FOUND));
        if (place.isDeleted()) {
            throw new GeneralException(PlaceErrorStatus.NOT_FOUND);
        }

        PlaceOperationPeriod operationPeriod = place.getOperationPeriod();
        PlaceTranslation translation = placeTranslationRepository
                .findByPlace_IdAndLanguageCode(placeId, LanguageCode.EN)
                .orElse(null);
        return new PlaceManagementDetail(
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getStation(),
                place.getStationDistanceMeters(),
                place.getType(),
                place.getThumbnailUrl(),
                operationPeriod == null ? null : operationPeriod.getStartDate(),
                operationPeriod == null ? null : operationPeriod.getEndDate(),
                place.dailySchedules(),
                translation == null ? null : translation.getTranslatedName(),
                translation == null ? null : translation.getTranslatedAddress(),
                translation == null ? null : translation.getTranslatedStation());
    }

    @Transactional
    public void update(Long placeId, PlaceUpsertCommand command, MultipartFile newThumbnail) {
        Place place =
                placeRepository.findById(placeId).orElseThrow(() -> new GeneralException(PlaceErrorStatus.NOT_FOUND));
        if (place.isDeleted()) {
            throw new GeneralException(PlaceErrorStatus.NOT_FOUND);
        }
        placeValidator.validateType(place.getType(), command.type());

        PlaceOperationPeriod requestedPeriod = command.type() == PlaceType.POPUP
                ? PlaceOperationPeriod.builder()
                        .startDate(command.operationStartDate())
                        .endDate(command.operationEndDate())
                        .build()
                : null;
        placeValidator.validateTranslations(command);
        placeValidator.validate(command.type(), requestedPeriod, command.dailySchedules());
        String thumbnailUrl = place.getThumbnailUrl();
        if (newThumbnail != null) {
            thumbnailUrl =
                    s3Service.uploadWithThumbnail(List.of(newThumbnail)).get(0).thumbnailUrl();
        }

        if (place.getType() == PlaceType.POPUP) {
            place.getOperationPeriod().revise(command.operationStartDate(), command.operationEndDate());
        }
        place.update(
                command.name(),
                command.address(),
                command.latitude(),
                command.longitude(),
                command.station(),
                command.stationDistanceMeters(),
                thumbnailUrl,
                place.getOperationPeriod());
        PlaceTranslation translation = placeTranslationRepository
                .findByPlace_IdAndLanguageCode(placeId, LanguageCode.EN)
                .orElse(null);
        if (command.nameEn() == null && command.addressEn() == null && command.stationEn() == null) {
            if (translation != null) {
                placeTranslationRepository.delete(translation);
            }
        } else if (translation == null) {
            placeTranslationRepository.save(PlaceTranslation.create(
                    place, LanguageCode.EN, command.nameEn(), command.addressEn(), command.stationEn()));
        } else {
            translation.revise(command.nameEn(), command.addressEn(), command.stationEn());
        }
        placeBusinessHourUpdater.update(place, command.dailySchedules(), LocalDateTime.now(clock));
        if (place.getType() == PlaceType.POPUP) {
            PlaceOperationPeriod operationPeriod = place.getOperationPeriod();
            List<PlaceDailySchedule> dailySchedules = place.dailySchedules();
            LocalDateTime closingAt = popupClosingAtCalculator.calculate(operationPeriod, dailySchedules);
            operationPeriod.scheduleClosingAt(closingAt);
        }
    }

    @Transactional
    public void delete(Long placeId) {
        Place place =
                placeRepository.findById(placeId).orElseThrow(() -> new GeneralException(PlaceErrorStatus.NOT_FOUND));
        if (!place.delete(LocalDateTime.now(clock))) {
            throw new GeneralException(PlaceErrorStatus.ALREADY_DELETED);
        }
    }

    @Transactional
    public void restore(Long placeId) {
        Place place =
                placeRepository.findById(placeId).orElseThrow(() -> new GeneralException(PlaceErrorStatus.NOT_FOUND));
        if (!place.restore()) {
            throw new GeneralException(PlaceErrorStatus.ALREADY_ACTIVE);
        }
    }
}
