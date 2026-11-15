package com.fmi.domain.place.repository;

import com.fmi.domain.place.data.PlaceFavorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceFavoriteRepository extends JpaRepository<PlaceFavorite, Long>, PlaceFavoriteRepositoryCustom {
    Optional<PlaceFavorite> findByUser_IdAndPlace_Id(Long userId, Long placeId);

    @Query(
            "select pf.place.id from PlaceFavorite pf where pf.user.id = :userId and pf.place.id in :placeIds and pf.favorite = true")
    List<Long> findFavoritePlaceIds(@Param("userId") Long userId, @Param("placeIds") List<Long> placeIds);
}
