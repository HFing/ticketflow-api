package com.hfing.ticketflowapi.event.entity;

import com.hfing.ticketflowapi.common.entity.BaseEntity;
import com.hfing.ticketflowapi.event.enums.EventShowStatus;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.*;

@Entity
@Table(name = "event_shows", indexes = {
        @Index(name = "idx_event_shows_event_id", columnList = "event_id"),
        @Index(name = "idx_event_shows_start_time", columnList = "start_time"),
        @Index(name = "idx_event_shows_sale_time", columnList = "sale_start_time, sale_end_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventShow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "sale_start_time", nullable = false)
    private LocalDateTime saleStartTime;

    @Column(name = "sale_end_time")
    private LocalDateTime saleEndTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventShowStatus status;

    @OneToMany(mappedBy = "eventShow", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TicketType> ticketTypes = new ArrayList<>();

    public boolean isOnSale(LocalDateTime now) {
        boolean afterSaleStart = !now.isBefore(saleStartTime);
        boolean beforeSaleEnd = saleEndTime == null || !now.isAfter(saleEndTime);

        return status != EventShowStatus.CANCELLED
                && status != EventShowStatus.COMPLETED
                && status != EventShowStatus.ENDED
                && afterSaleStart
                && beforeSaleEnd;
    }

}
