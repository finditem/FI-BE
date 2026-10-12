package com.fmi.domain.place.web.dto.request;

import com.fmi.domain.place.data.enums.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceMapSearchRequest {

    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") private Double latitude;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double longitude;

    @Min(1) @Max(8) private int level = 6;

    @NotNull private PlaceType type;

    @Schema(
            description = "장소 문자열의 언어. 생략 시 KO, 빈 값과 소문자는 허용하지 않습니다.",
            allowableValues = {"KO", "EN"},
            defaultValue = "KO")
    private String language;
}
