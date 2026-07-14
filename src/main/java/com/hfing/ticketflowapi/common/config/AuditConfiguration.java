package com.hfing.ticketflowapi.common.config;

import java.util.Optional;
import lombok.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;




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