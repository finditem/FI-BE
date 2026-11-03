package com.fmi.domain.user.temperature.repository;

import com.fmi.domain.user.temperature.data.TemperatureActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemperatureActivityRepository extends JpaRepository<TemperatureActivity, Long> {}
