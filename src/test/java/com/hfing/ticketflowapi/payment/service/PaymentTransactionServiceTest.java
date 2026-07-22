package com.hfing.ticketflowapi.payment.service;

import com.hfing.ticketflowapi.booking.dto.request.CheckoutItemRequest;
import com.hfing.ticketflowapi.booking.dto.request.CheckoutRequest;
import com.hfing.ticketflowapi.booking.entity.Booking;
import com.hfing.ticketflowapi.booking.enums.BookingStatus;
import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.entity.EventShow;
import com.hfing.ticketflowapi.event.entity.TicketType;
import com.hfing.ticketflowapi.event.enums.EventShowStatus;
import com.hfing.ticketflowapi.event.enums.EventStatus;
import com.hfing.ticketflowapi.event.enums.TicketTypeStatus;
import com.hfing.ticketflowapi.event.repository.EventShowRepository;
import com.hfing.ticketflowapi.event.repository.TicketTypeRepository;
import com.hfing.ticketflowapi.payment.entity.Payment;
import com.hfing.ticketflowapi.payment.enums.PaymentStatus;
import com.hfing.ticketflowapi.payment.mapper.PaymentMapper;
import com.hfing.ticketflowapi.booking.repository.BookingRepository;
import com.hfing.ticketflowapi.user.entity.User;
import com.hfing.ticketflowapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {

}
