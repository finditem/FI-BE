package com.fmi.domain.report.listener;

import com.fmi.domain.report.event.ReportEvent;
import com.fmi.global.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReportEventListener {

    private final SlackService slackService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReportEvent(ReportEvent event) {
        slackService.sendReportNotification(event);
    }
}
