package com.fmi.domain.place.data;

import com.fmi.domain.Enum.LanguageCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "place_translation", uniqueConstraints = @UniqueConstraint(columnNames = {"place_id", "language_code"}))
public class PlaceTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_code", nullable = false, length = 10)
    private LanguageCode languageCode;

    @Column(name = "translated_name", length = 100)
    private String translatedName;

    @Column(name = "translated_address", length = 510)
    private String translatedAddress;

    @Column(name = "translated_station", length = 200)
    private String translatedStation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static PlaceTranslation create(
            Place place, LanguageCode language, String name, String address, String station) {
        LocalDateTime now = LocalDateTime.now();
        return PlaceTranslation.builder()
                .place(place)
                .languageCode(language)
                .translatedName(name)
                .translatedAddress(address)
                .translatedStation(station)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void revise(String name, String address, String station) {
        translatedName = name;
        translatedAddress = address;
        translatedStation = station;
        updatedAt = LocalDateTime.now();
    }
}
