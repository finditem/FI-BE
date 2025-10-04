package com.fmi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    public void sendEmail(String to, String subject, String body) {
        // 실제 메일 발송 대신 로깅으로 대체 (운영에서는 SMTP 구성 필요)
        log.info("[EMAIL] to={} subject={} body=\n{}", to, subject, body);
    }
}


