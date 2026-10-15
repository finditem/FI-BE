package com.fmi.domain.temperature.repository;

import com.fmi.domain.temperature.data.TemperatureActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemperatureActivityRepository extends JpaRepository<TemperatureActivity, Long> {}
