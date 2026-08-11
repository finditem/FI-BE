package com.fmi.domain.report.listener;

import com.fmi.domain.report.event.ReportEvent;
import com.fmi.global.service.SlackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportListener {

    private final SlackService slackService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportCreated(ReportEvent event) {
        try {
            slackService.sendReportNotification(event);
        } catch (Exception e) {
            log.error("신고 슬랙 알림 전송 실패. reportId={}", event.reportId(), e);
        }
    }
}
