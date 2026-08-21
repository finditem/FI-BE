package com.fmi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    public void send(String to, String message) {
        // 실제 SMS 연동 대신 로깅으로 대체 (운영에서는 통신사/서드파티 연동 필요)
        log.info("[SMS] to={} message=\n{}", to, message);
    }
}
