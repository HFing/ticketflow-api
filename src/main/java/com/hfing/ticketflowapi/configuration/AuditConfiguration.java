package com.hfing.ticketflowapi.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.NonNull;

import java.util.Optional;

@Configuration
public class AuditConfiguration implements AuditorAware<String> {

    // @Override
    // public Optional<String> getCurrentAuditor() {
    // return Optional.empty();
    // }
    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getName() != null
                ? Optional.ofNullable(authentication.getName())
                : Optional.empty();
    }
}
