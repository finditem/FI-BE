package com.fmi.domain.place.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoritePlaceRequest {

    private LocalDateTime lastFavoriteUpdatedAt;

    @Min(1) @Max(20) private int size = 20;

    @Schema(
            description = "장소 문자열의 언어. 생략 시 KO, 빈 값과 소문자는 허용하지 않습니다.",
            allowableValues = {"KO", "EN"},
            defaultValue = "KO")
    private String language;
}
