package com.fmi.domain.place.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.user.user.data.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("장소 즐겨찾기")
class PlaceFavoriteTest {
    @Test
    @DisplayName("신규 즐겨찾기는 활성 상태다")
    void startsFavorite() {
        assertThat(PlaceFavorite.builder()
                        .user(User.builder().id(1L).build())
                        .place(Place.builder().build())
                        .build()
                        .isFavorite())
                .isTrue();
    }

    @Test
    @DisplayName("즐겨찾기를 취소하고 다시 활성화한다")
    void togglesFavorite() {
        PlaceFavorite favorite = PlaceFavorite.builder()
                .user(User.builder().id(1L).build())
                .place(Place.builder().build())
                .build();
        assertThat(favorite.unfavorite()).isTrue();
        assertThat(favorite.unfavorite()).isFalse();
        assertThat(favorite.favorite()).isTrue();
        assertThat(favorite.favorite()).isFalse();
    }
}
