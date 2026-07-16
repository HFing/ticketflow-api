package com.hfing.ticketflowapi.user.config;

import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.entity.EventShow;
import com.hfing.ticketflowapi.event.entity.TicketType;
import com.hfing.ticketflowapi.event.enums.EventCategory;
import com.hfing.ticketflowapi.event.enums.EventShowStatus;
import com.hfing.ticketflowapi.event.enums.EventStatus;
import com.hfing.ticketflowapi.event.enums.TicketTypeStatus;
import com.hfing.ticketflowapi.event.repository.EventRepository;
import com.hfing.ticketflowapi.user.entity.Role;
import com.hfing.ticketflowapi.user.entity.User;
import com.hfing.ticketflowapi.user.enums.RoleType;
import com.hfing.ticketflowapi.user.repository.RoleRepository;
import com.hfing.ticketflowapi.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    private final EventRepository eventRepository;
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

        seedDemoEvents();
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

    private void seedDemoEvents() {
        User organizer = userRepository.findByEmail("organizer@ticketflow.com")
                .orElseThrow(() -> new RuntimeException("Default organizer not found"));

        createDemoEvent(
                organizer,
                "Saigon Indie Night 2026",
                "A cozy live music night with indie bands, acoustic sets, and late-summer city energy.",
                "District 1, Ho Chi Minh City",
                "Lotus Stage",
                "https://images.unsplash.com/photo-1501386761578-eac5c94b800a",
                "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f",
                EventCategory.LIVE_MUSIC,
                true,
                List.of(
                        new ShowSeed(
                                LocalDateTime.of(2026, 8, 15, 19, 30),
                                LocalDateTime.of(2026, 8, 15, 22, 30),
                                LocalDateTime.of(2026, 7, 18, 9, 0),
                                LocalDateTime.of(2026, 8, 15, 18, 0),
                                List.of(
                                        new TicketSeed("Early Bird", "Limited early access ticket", "199000", 120, 4),
                                        new TicketSeed("Standard", "General admission ticket", "299000", 400, 6),
                                        new TicketSeed("VIP Lounge", "Best view area with welcome drink", "699000", 60, 2)
                                )
                        )
                )
        );

        createDemoEvent(
                organizer,
                "Hanoi Jazz Rooftop",
                "An evening of modern jazz, city lights, and curated drinks on an open-air rooftop.",
                "Hoan Kiem, Hanoi",
                "Skyline Rooftop",
                "https://images.unsplash.com/photo-1415201364774-f6f0bb35f28f",
                "https://images.unsplash.com/photo-1511192336575-5a79af67a629",
                EventCategory.LIVE_MUSIC,
                true,
                List.of(
                        new ShowSeed(
                                LocalDateTime.of(2026, 8, 22, 20, 0),
                                LocalDateTime.of(2026, 8, 22, 23, 0),
                                LocalDateTime.of(2026, 7, 20, 10, 0),
                                LocalDateTime.of(2026, 8, 22, 19, 0),
                                List.of(
                                        new TicketSeed("Standard", "Rooftop standing ticket", "350000", 250, 4),
                                        new TicketSeed("Table Seat", "Reserved table seat", "650000", 80, 4)
                                )
                        )
                )
        );

        createDemoEvent(
                organizer,
                "The Little Prince: Stage Adaptation",
                "A poetic stage adaptation with music, lighting design, and family-friendly storytelling.",
                "District 3, Ho Chi Minh City",
                "Aurora Theater",
                "https://images.unsplash.com/photo-1503095396549-807759245b35",
                "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf",
                EventCategory.STAGE_ART,
                false,
                List.of(
                        new ShowSeed(
                                LocalDateTime.of(2026, 9, 5, 18, 0),
                                LocalDateTime.of(2026, 9, 5, 20, 0),
                                LocalDateTime.of(2026, 7, 25, 8, 0),
                                LocalDateTime.of(2026, 9, 5, 16, 0),
                                List.of(
                                        new TicketSeed("Balcony", "Balcony seat", "180000", 120, 6),
                                        new TicketSeed("Standard Seat", "Main hall seat", "280000", 260, 6),
                                        new TicketSeed("Premium Seat", "Front rows", "450000", 80, 4)
                                )
                        ),
                        new ShowSeed(
                                LocalDateTime.of(2026, 9, 6, 18, 0),
                                LocalDateTime.of(2026, 9, 6, 20, 0),
                                LocalDateTime.of(2026, 7, 25, 8, 0),
                                LocalDateTime.of(2026, 9, 6, 16, 0),
                                List.of(
                                        new TicketSeed("Standard Seat", "Main hall seat", "280000", 260, 6),
                                        new TicketSeed("Premium Seat", "Front rows", "450000", 80, 4)
                                )
                        )
                )
        );

        createDemoEvent(
                organizer,
                "Product Design Sprint Bootcamp",
                "A hands-on workshop for mapping product problems, prototyping, and testing ideas fast.",
                "Binh Thanh, Ho Chi Minh City",
                "Canvas Innovation Hub",
                "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4",
                "https://images.unsplash.com/photo-1552664730-d307ca884978",
                EventCategory.WORKSHOP,
                false,
                List.of(
                        new ShowSeed(
                                LocalDateTime.of(2026, 8, 29, 9, 0),
                                LocalDateTime.of(2026, 8, 29, 17, 0),
                                LocalDateTime.of(2026, 7, 19, 9, 0),
                                LocalDateTime.of(2026, 8, 28, 18, 0),
                                List.of(
                                        new TicketSeed("Individual", "One workshop pass", "590000", 80, 2),
                                        new TicketSeed("Team Pass", "Team ticket for up to 4 people", "1990000", 25, 2)
                                )
                        )
                )
        );

        createDemoEvent(
                organizer,
                "Danang Sunset Run",
                "A coastal running event with 5K, 10K, and half-marathon routes at sunset.",
                "My Khe Beach, Da Nang",
                "My Khe Beach Park",
                "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8",
                "https://images.unsplash.com/photo-1461896836934-ffe607ba8211",
                EventCategory.SPORTS,
                true,
                List.of(
                        new ShowSeed(
                                LocalDateTime.of(2026, 10, 4, 15, 30),
                                LocalDateTime.of(2026, 10, 4, 20, 0),
                                LocalDateTime.of(2026, 7, 21, 9, 0),
                                LocalDateTime.of(2026, 9, 25, 23, 59),
                                List.of(
                                        new TicketSeed("5K Runner", "Race kit for 5K route", "250000", 600, 4),
                                        new TicketSeed("10K Runner", "Race kit for 10K route", "390000", 500, 4),
                                        new TicketSeed("Half Marathon", "Race kit for 21K route", "650000", 300, 2)
                                )
                        )
                )
        );

        createDemoEvent(
                organizer,
                "Mekong Taste Journey",
                "A food and culture experience featuring regional dishes, live demos, and local makers.",
                "Can Tho",
                "River Market Hall",
                "https://images.unsplash.com/photo-1555939594-58d7cb561ad1",
                "https://images.unsplash.com/photo-1504674900247-0877df9cc836",
                EventCategory.EXPERIENCE,
                false,
                List.of(
                        new ShowSeed(
                                LocalDateTime.of(2026, 9, 19, 10, 0),
                                LocalDateTime.of(2026, 9, 19, 15, 0),
                                LocalDateTime.of(2026, 7, 22, 9, 0),
                                LocalDateTime.of(2026, 9, 18, 18, 0),
                                List.of(
                                        new TicketSeed("Tasting Pass", "Access to tasting counters", "320000", 250, 4),
                                        new TicketSeed("Workshop Combo", "Tasting pass plus cooking demo", "520000", 80, 2)
                                )
                        )
                )
        );

        createDemoEvent(
                organizer,
                "Future Tech Expo Vietnam",
                "A technology expo featuring AI showcases, startup demos, and expert panels.",
                "District 7, Ho Chi Minh City",
                "SECC Hall B",
                "https://images.unsplash.com/photo-1540575467063-178a50c2df87",
                "https://images.unsplash.com/photo-1519389950473-47ba0277781c",
                EventCategory.WORKSHOP,
                true,
                List.of(
                        new ShowSeed(
                                LocalDateTime.of(2026, 11, 12, 9, 0),
                                LocalDateTime.of(2026, 11, 12, 18, 0),
                                LocalDateTime.of(2026, 8, 1, 9, 0),
                                LocalDateTime.of(2026, 11, 11, 23, 0),
                                List.of(
                                        new TicketSeed("Expo Pass", "One-day expo access", "150000", 1000, 10),
                                        new TicketSeed("Conference Pass", "Expo and conference access", "750000", 350, 4),
                                        new TicketSeed("Business Pass", "Conference access and networking lounge", "1500000", 120, 2)
                                )
                        )
                )
        );

        createDemoEvent(
                organizer,
                "Moonlight Cinema Weekend",
                "An outdoor movie weekend with picnic zones, food stalls, and classic films under the sky.",
                "Thu Duc City, Ho Chi Minh City",
                "Greenfield Park",
                "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba",
                "https://images.unsplash.com/photo-1524985069026-dd778a71c7b4",
                EventCategory.EXPERIENCE,
                false,
                List.of(
                        new ShowSeed(
                                LocalDateTime.of(2026, 8, 30, 18, 30),
                                LocalDateTime.of(2026, 8, 30, 22, 0),
                                LocalDateTime.of(2026, 7, 18, 9, 0),
                                LocalDateTime.of(2026, 8, 30, 16, 0),
                                List.of(
                                        new TicketSeed("Lawn Seat", "Outdoor cinema access", "120000", 500, 6),
                                        new TicketSeed("Picnic Combo", "Entry plus picnic set for two", "290000", 150, 3)
                                )
                        )
                )
        );
    }

    private void createDemoEvent(
            User organizer,
            String name,
            String description,
            String location,
            String venue,
            String bannerUrl,
            String shortImageUrl,
            EventCategory category,
            boolean isHot,
            List<ShowSeed> showSeeds) {
        if (eventRepository.existsByName(name)) {
            return;
        }

        Event event = Event.builder()
                .name(name)
                .description(description)
                .location(location)
                .venue(venue)
                .bannerUrl(bannerUrl)
                .shortImageUrl(shortImageUrl)
                .category(category)
                .status(EventStatus.PUBLISHED)
                .isHot(isHot)
                .organizer(organizer)
                .build();

        for (ShowSeed showSeed : showSeeds) {
            EventShow show = EventShow.builder()
                    .startTime(showSeed.startTime())
                    .endTime(showSeed.endTime())
                    .saleStartTime(showSeed.saleStartTime())
                    .saleEndTime(showSeed.saleEndTime())
                    .status(EventShowStatus.ON_SALE)
                    .event(event)
                    .build();

            for (TicketSeed ticketSeed : showSeed.tickets()) {
                TicketType ticketType = TicketType.builder()
                        .name(ticketSeed.name())
                        .description(ticketSeed.description())
                        .price(new BigDecimal(ticketSeed.price()))
                        .totalQuantity(ticketSeed.totalQuantity())
                        .soldQuantity(0)
                        .heldQuantity(0)
                        .maxPerOrder(ticketSeed.maxPerOrder())
                        .status(TicketTypeStatus.ACTIVE)
                        .eventShow(show)
                        .build();
                show.getTicketTypes().add(ticketType);
            }

            event.getShows().add(show);
        }

        eventRepository.save(event);
    }

    private record ShowSeed(
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime saleStartTime,
            LocalDateTime saleEndTime,
            List<TicketSeed> tickets) {
    }

    private record TicketSeed(
            String name,
            String description,
            String price,
            Integer totalQuantity,
            Integer maxPerOrder) {
    }
}
