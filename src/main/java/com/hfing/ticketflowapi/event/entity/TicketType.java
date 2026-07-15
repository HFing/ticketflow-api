package com.hfing.ticketflowapi.event.entity;

import com.hfing.ticketflowapi.common.entity.BaseEntity;
import com.hfing.ticketflowapi.event.enums.TicketTypeStatus;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ticket_types", indexes = {
        @Index(name = "idx_ticket_types_event_show_id", columnList = "event_show_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity;

    @Column(name = "held_quantity", nullable = false)
    private Integer heldQuantity;

    @Column(name = "max_per_order", nullable = false)
    private Integer maxPerOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketTypeStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_show_id", nullable = false)
    private EventShow eventShow;

    public Integer getAvailableQuantity() {
        return totalQuantity - soldQuantity - heldQuantity;
    }

    public boolean isAvailable() {
        return status == TicketTypeStatus.ACTIVE && getAvailableQuantity() > 0;
    }
}
