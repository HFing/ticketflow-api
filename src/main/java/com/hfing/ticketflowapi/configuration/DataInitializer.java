package com.hfing.ticketflowapi.configuration;

import com.hfing.ticketflowapi.common.enums.RoleType;
import com.hfing.ticketflowapi.entity.Role;
import com.hfing.ticketflowapi.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        for (RoleType roleType : RoleType.values()) {
            if (!roleRepository.existsByName(roleType.name())) {
                Role role = Role.builder()
                        .name(roleType.name())
                        .description(roleType.name() + " role")
                        .build();

                roleRepository.save(role);
            }
        }
    }
}