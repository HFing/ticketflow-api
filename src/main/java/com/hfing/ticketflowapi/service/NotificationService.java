package com.hfing.ticketflowapi.service;

import com.hfing.ticketflowapi.dto.event.UserRegisteredEvent;

public interface NotificationService {
    void sendRegistrationEmail(UserRegisteredEvent event);
}
