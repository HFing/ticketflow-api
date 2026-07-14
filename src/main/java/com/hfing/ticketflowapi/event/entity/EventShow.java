package com.hfing.ticketflowapi.event.entity;

import com.hfing.ticketflowapi.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;



@Entity
@Table(name = "event_shows")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
}