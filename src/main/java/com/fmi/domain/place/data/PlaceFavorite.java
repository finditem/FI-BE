package com.fmi.domain.place.data;

import com.fmi.domain.user.user.data.User;
import com.fmi.global.data.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "place_favorite",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_place_favorite_user_place",
                        columnNames = {"user_id", "place_id"}))
public class PlaceFavorite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite;

    @Builder
    private PlaceFavorite(User user, Place place) {
        this.user = user;
        this.place = place;
        this.favorite = true;
    }

    public boolean favorite() {
        if (favorite) {
            return false;
        }
        favorite = true;
        return true;
    }

    public boolean unfavorite() {
        if (!favorite) {
            return false;
        }
        favorite = false;
        return true;
    }
}
