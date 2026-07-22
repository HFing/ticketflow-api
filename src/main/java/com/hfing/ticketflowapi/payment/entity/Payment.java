package com.hfing.ticketflowapi.payment.entity;

import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.payment.enums.PaymentProvider;
import com.hfing.ticketflowapi.payment.enums.PaymentStatus;
import com.hfing.ticketflowapi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_payments_booking", columnNames = "booking_id"),
        @UniqueConstraint(
                name = "uq_payments_provider_payment_id",
                columnNames = {"provider", "provider_payment_id"}),
        @UniqueConstraint(
                name = "uq_payments_provider_session_id",
                columnNames = {"provider", "provider_session_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    @Column(name = "provider_session_id", length = 100)
    private String providerSessionId;

    @Column(name = "paid_at")
    private Instant paidAt;
}
