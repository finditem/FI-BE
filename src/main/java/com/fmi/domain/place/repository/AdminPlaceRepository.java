package com.fmi.domain.place.repository;

import com.fmi.domain.place.data.Place;
import org.springframework.data.repository.Repository;

public interface AdminPlaceRepository extends Repository<Place, Long>, AdminPlaceRepositoryCustom {}
