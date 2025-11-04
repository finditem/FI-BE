package com.fmi.domain.favoritepost.repository;

import com.fmi.domain.favoritepost.data.FavoritePost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoritePost,Long> {

}
