package com.zwei.iot.alarm.channel;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NotifyRecipientValidatorTest {

    @Test
    void phone_null_returns_missing() {
        assertThat(NotifyRecipientValidator.validatePhone(null))
            .isEqualTo("RECIPIENT_PHONE_MISSING");
    }

    @Test
    void phone_blank_returns_missing() {
        assertThat(NotifyRecipientValidator.validatePhone("  "))
            .isEqualTo("RECIPIENT_PHONE_MISSING");
    }

    @Test
    void phone_short_returns_invalid() {
        assertThat(NotifyRecipientValidator.validatePhone("1380"))
            .isEqualTo("RECIPIENT_PHONE_INVALID");
    }

    @Test
    void phone_invalid_prefix_returns_invalid() {
        assertThat(NotifyRecipientValidator.validatePhone("12812345678"))
            .isEqualTo("RECIPIENT_PHONE_INVALID");
    }

    @Test
    void phone_valid_returns_null() {
        assertThat(NotifyRecipientValidator.validatePhone("13812345678"))
            .isNull();
    }

    @Test
    void email_null_returns_missing() {
        assertThat(NotifyRecipientValidator.validateEmail(null))
            .isEqualTo("RECIPIENT_EMAIL_MISSING");
    }

    @Test
    void email_no_at_returns_invalid() {
        assertThat(NotifyRecipientValidator.validateEmail("abc.example.com"))
            .isEqualTo("RECIPIENT_EMAIL_INVALID");
    }

    @Test
    void email_valid_returns_null() {
        assertThat(NotifyRecipientValidator.validateEmail("abc@example.com"))
            .isNull();
    }
}
