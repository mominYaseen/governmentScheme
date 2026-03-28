package com.schemenavigator.service;

import com.schemenavigator.model.UserSavedScheme;
import com.schemenavigator.repository.UserSavedSchemeRepository;
import com.schemenavigator.util.ApplyUrlExtractor;
import com.schemenavigator.config.ReminderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemeReminderService {

    private final UserSavedSchemeRepository userSavedSchemeRepository;
    private final ReminderProperties reminderProperties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String springMailUsername;

    @Transactional
    public void sendDueReminders() {
        if (!reminderProperties.isEnabled()) {
            return;
        }
        Instant now = Instant.now();
        List<UserSavedScheme> due = userSavedSchemeRepository.findDueForReminder(now);
        if (due.isEmpty()) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Reminder job skipped: no JavaMailSender (configure spring.mail.host to enable email). {} due row(s).",
                    due.size());
            return;
        }
        String from = resolveFrom();
        if (from == null || from.isBlank()) {
            log.warn("Reminder job skipped: set app.reminder.mail-from or spring.mail.username");
            return;
        }
        for (UserSavedScheme row : due) {
            try {
                sendOne(mailSender, from, row);
                row.setLastReminderSentAt(now);
                row.setNextReminderAt(row.isRemindEnabled()
                        ? now.plus(reminderProperties.getIntervalDays(), ChronoUnit.DAYS)
                        : null);
                userSavedSchemeRepository.save(row);
            } catch (Exception e) {
                log.error("Failed reminder for user {} scheme {}", row.getUser().getId(), row.getScheme().getId(), e);
            }
        }
    }

    private void sendOne(JavaMailSender mailSender, String from, UserSavedScheme row) {
        var scheme = row.getScheme();
        var user = row.getUser();
        String applyUrl = ApplyUrlExtractor.resolve(
                scheme.getApplyUrl(), scheme.getApplyProcess(), scheme.getDescription(), scheme.getBenefits());
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(user.getEmail());
        msg.setSubject("Reminder: apply to \"" + scheme.getName() + "\"");
        StringBuilder body = new StringBuilder();
        body.append("You saved this scheme in Scheme Navigator.\n\n");
        body.append(scheme.getName()).append("\n\n");
        if (applyUrl != null) {
            body.append("Apply / official link: ").append(applyUrl).append("\n");
        } else {
            body.append("No apply URL is on file — open the app for full instructions.\n");
        }
        body.append("\nYou can turn off reminders by updating the saved scheme in your inventory.");
        msg.setText(body.toString());
        mailSender.send(msg);
    }

    private String resolveFrom() {
        if (reminderProperties.getMailFrom() != null && !reminderProperties.getMailFrom().isBlank()) {
            return reminderProperties.getMailFrom().trim();
        }
        if (springMailUsername != null && !springMailUsername.isBlank()) {
            return springMailUsername.trim();
        }
        return null;
    }
}
