package com.hfing.ticketflowapi.payment.scheduler;

import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import com.hfing.ticketflowapi.booking.repository.BookingRepository;
import com.hfing.ticketflowapi.booking.service.IBookingService;
import com.hfing.ticketflowapi.common.exception.AppException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-EXPIRATION-SCHEDULER")
public class PaymentExpirationScheduler {
    private final BookingRepository bookingRepository;
    private final IBookingService bookingService;

    @Scheduled(fixedDelayString = "${vnpay.expiration-scan-interval-ms:10000}")
    public void expireHeldInventory() {
        Instant now = Instant.now();
        bookingRepository.findExpiredBookingIds(
                        BookingStatus.PENDING_PAYMENT,
                        now,
                        PageRequest.of(0, 100))
                .forEach(bookingId -> expireOne(bookingId, now));
    }

    private void expireOne(String bookingId, Instant now) {
        try {
            bookingService.expirePendingBooking(bookingId, now);
        } catch (AppException exception) {
            log.warn("Booking expiration will be retried for bookingId={}, reason={}",
                    bookingId, exception.getMessage());
        }
    }
}
