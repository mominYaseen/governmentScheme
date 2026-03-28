package com.schemenavigator.schedule;

import com.schemenavigator.service.SchemeReminderService;
import com.schemenavigator.config.ReminderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchemeReminderScheduler {

    private final SchemeReminderService schemeReminderService;
    private final ReminderProperties reminderProperties;

    @Scheduled(cron = "${app.reminder.cron:0 0 9 * * *}")
    public void runReminders() {
        if (!reminderProperties.isEnabled()) {
            return;
        }
        try {
            schemeReminderService.sendDueReminders();
        } catch (Exception e) {
            log.error("Reminder batch failed", e);
        }
    }
}
