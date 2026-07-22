package com.hfing.ticketflowapi.payment.scheduler;

import com.hfing.ticketflowapi.booking.repository.BookingRepository;
import com.hfing.ticketflowapi.booking.service.IBookingService;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentExpirationSchedulerTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private IBookingService bookingService;
    @InjectMocks private PaymentExpirationScheduler scheduler;

    @Test
    void expiresEveryCandidateAndContinuesWhenOneBookingFails() {
        when(bookingRepository.findExpiredBookingIds(any(), any(), any(Pageable.class)))
                .thenReturn(List.of("booking-1", "booking-2"));
        doThrow(new AppException(ErrorCode.PAYMENT_INVALID_STATE))
                .when(bookingService).expirePendingBooking(eq("booking-1"), any(Instant.class));

        scheduler.expireHeldInventory();

        verify(bookingService).expirePendingBooking(eq("booking-1"), any(Instant.class));
        verify(bookingService).expirePendingBooking(eq("booking-2"), any(Instant.class));
    }
}
