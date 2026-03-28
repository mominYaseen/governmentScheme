package com.schemenavigator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.reminder")
public class ReminderProperties {

    private boolean enabled = true;

    /** Days between reminder emails for each saved scheme. */
    private int intervalDays = 7;

    /** Spring cron (server timezone). Default: daily 09:00. */
    private String cron = "0 0 9 * * *";

    /** From header when sending mail (falls back to spring.mail.username). */
    private String mailFrom = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(int intervalDays) {
        this.intervalDays = intervalDays;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }
}
