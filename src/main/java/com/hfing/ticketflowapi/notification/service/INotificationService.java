package com.hfing.ticketflowapi.notification.service;

import com.hfing.ticketflowapi.notification.dto.UserRegisteredEvent;

public interface INotificationService {
    void sendRegistrationEmail(UserRegisteredEvent event);
}
