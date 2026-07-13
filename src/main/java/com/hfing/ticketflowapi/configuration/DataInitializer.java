package com.hfing.ticketflowapi.configuration;

import com.hfing.ticketflowapi.common.enums.RoleType;
import com.hfing.ticketflowapi.entity.Role;
import com.hfing.ticketflowapi.entity.User;
import com.hfing.ticketflowapi.repository.RoleRepository;
import com.hfing.ticketflowapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

        createDefaultUser("admin@ticketflow.com", "Admin@123", "Admin", "System", "0123456789", RoleType.ADMIN);
        createDefaultUser("organizer@ticketflow.com", "Organizer@123", "Organizer", "Event", "0123456789",
                RoleType.ORGANIZER);
        createDefaultUser("customer@ticketflow.com", "Customer@123", "Customer", "User", "0123456789",
                RoleType.CUSTOMER);
    }

    private void createDefaultUser(String email, String rawPassword, String firstName, String lastName, String phone,
            RoleType roleType) {
        if (!userRepository.existsByEmail(email)) {
            Role role = roleRepository.findByName(roleType.name())
                    .orElseThrow(() -> new RuntimeException("Role " + roleType.name() + " not found"));

            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .firstName(firstName)
                    .lastName(lastName)
                    .phone(phone)
                    .role(role)
                    .build();

            userRepository.save(user);
        }
    }
}
