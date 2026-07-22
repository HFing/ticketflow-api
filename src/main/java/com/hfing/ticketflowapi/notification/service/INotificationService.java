package com.hfing.ticketflowapi.notification.service;

import com.hfing.ticketflowapi.notification.dto.UserRegisteredEvent;
import com.hfing.ticketflowapi.notification.dto.PaymentCompletedEvent;

public interface INotificationService {
    void sendRegistrationEmail(UserRegisteredEvent event);

    void sendPaymentConfirmationEmail(PaymentCompletedEvent event);
}
