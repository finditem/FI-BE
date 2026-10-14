package com.fmi.domain.temperature.service;

import com.fmi.domain.temperature.data.TemperatureSnapshot;
import com.fmi.domain.temperature.data.UserTemperature;
import com.fmi.domain.temperature.repository.UserTemperatureRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemperatureQueryService {

    private final UserRepository userRepository;
    private final UserTemperatureRepository userTemperatureRepository;

    // 가장 최근 정기 계산 결과를 보여준다. 계산된 적 없으면 초기 온도다.
    public TemperatureSnapshot getMyTemperature(String userEmail) {
        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        return userTemperatureRepository
                .findById(user.getId())
                .map(UserTemperature::toSnapshot)
                .orElseGet(TemperatureSnapshot::initial);
    }
}
