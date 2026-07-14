package com.hfing.ticketflowapi.event.service.impl;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.event.dto.*;
import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.entity.EventShow;
import com.hfing.ticketflowapi.event.entity.TicketType;
import com.hfing.ticketflowapi.event.enums.EventShowStatus;
import com.hfing.ticketflowapi.event.enums.EventStatus;
import com.hfing.ticketflowapi.event.enums.TicketTypeStatus;
import com.hfing.ticketflowapi.event.mapper.EventMapper;
import com.hfing.ticketflowapi.event.mapper.EventShowMapper;
import com.hfing.ticketflowapi.event.mapper.TicketTypeMapper;
import com.hfing.ticketflowapi.event.repository.EventRepository;
import com.hfing.ticketflowapi.event.repository.EventShowRepository;
import com.hfing.ticketflowapi.event.repository.TicketTypeRepository;
import com.hfing.ticketflowapi.event.service.EventService;
import com.hfing.ticketflowapi.user.entity.User;
import com.hfing.ticketflowapi.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final EventShowRepository eventShowRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final EventShowMapper eventShowMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final CacheManager cacheManager;

    @Autowired
    @Lazy
    private EventService self; // Self-injection to enable proxy invocation for @Cacheable

    @Override
    @Transactional
    @CacheEvict(value = "eventsList", allEntries = true)
    public EventResponse createEvent(CreateEventRequest request, String currentUserId) {
        User organizer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Event event = eventMapper.toEvent(request);
        event.setStatus(EventStatus.DRAFT);
        event.setOrganizer(organizer);

        Event savedEvent = eventRepository.save(event);
        return eventMapper.toEventResponse(savedEvent);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "eventsList", allEntries = true),
            @CacheEvict(value = "eventDetail", key = "#id")
    })
    public EventResponse updateEvent(String id, UpdateEventRequest request, String currentUserId, String role) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        authorizeModification(event, currentUserId, role);

        eventMapper.updateEventFromRequest(request, event);

        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toEventResponse(updatedEvent);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "eventsList", allEntries = true),
            @CacheEvict(value = "eventDetail", key = "#id")
    })
    public void deleteEvent(String id, String currentUserId, String role) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        authorizeModification(event, currentUserId, role);

        eventRepository.delete(event);
    }


    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "eventsList", allEntries = true),
            @CacheEvict(value = "eventDetail", key = "#id")
    })
    public EventResponse cancelEvent(String id, String currentUserId, String role) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        authorizeModification(event, currentUserId, role);

        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            throw new AppException(ErrorCode.EVENT_NOT_DRAFT); // Or general validation error
        }

        event.setStatus(EventStatus.CANCELLED);
        Event cancelledEvent = eventRepository.save(event);
        return eventMapper.toEventResponse(cancelledEvent);
    }

    private void authorizeModification(Event event, String currentUserId, String role) {
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("ORGANIZER".equals(role)) {
            if (event.getOrganizer() == null || !event.getOrganizer().getId().equals(currentUserId)) {
                throw new AppException(ErrorCode.EVENT_FORBIDDEN_MODIFICATION);
            }
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN);
    }

    @Override
    @Transactional
    public EventShowResponse createShow(String eventId, CreateEventShowRequest request, String currentUserId,
            String role) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        authorizeModification(event, currentUserId, role);

        EventShow show = EventShow.builder()
                .startTime(request.startTime())
                .endTime(request.endTime())
                .saleStartTime(request.saleStartTime())
                .saleEndTime(request.saleEndTime())
                .status(EventShowStatus.SCHEDULED)
                .event(event)
                .build();

        event.getShows().add(show);

        EventShow savedShow = eventShowRepository.save(show);

        evictEventDetailCache(eventId);

        return eventShowMapper.toEventShowResponse(savedShow);
    }

    @Override
    @Transactional
    public TicketTypeResponse createTicketType(String showId, CreateTicketTypeRequest request, String currentUserId,
            String role) {
        EventShow show = eventShowRepository.findById(showId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_FOUND));

        Event event = show.getEvent();
        authorizeModification(event, currentUserId, role);

        TicketType ticketType = TicketType.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .totalQuantity(request.totalQuantity())
                .soldQuantity(0)
                .heldQuantity(0)
                .maxPerOrder(request.maxPerOrder())
                .status(TicketTypeStatus.ACTIVE)
                .eventShow(show)
                .build();

        show.getTicketTypes().add(ticketType);
        eventShowRepository.save(show);
        TicketType savedTicket = ticketTypeRepository.save(ticketType);

        evictEventDetailCache(event.getId());

        return ticketTypeMapper.toTicketTypeResponse(savedTicket);
    }

    @Override
    @Transactional
    public EventResponse submitForReview(String eventId, String currentUserId, String role) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        authorizeModification(event, currentUserId, role);

        if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.REJECTED) {
            throw new AppException(ErrorCode.EVENT_NOT_DRAFT_OR_REJECTED);
        }

        // Validate Event has at least 1 show
        if (event.getShows() == null || event.getShows().isEmpty()) {
            throw new AppException(ErrorCode.EVENT_NO_SHOWS);
        }

        // Validate each show and its ticket types
        for (EventShow show : event.getShows()) {
            if (show.getTicketTypes() == null || show.getTicketTypes().isEmpty()) {
                throw new AppException(ErrorCode.SHOW_NO_TICKET_TYPES);
            }

            LocalDateTime start = show.getStartTime();
            LocalDateTime end = show.getEndTime();
            LocalDateTime saleStart = show.getSaleStartTime();
            LocalDateTime saleEnd = show.getSaleEndTime();

            if (!start.isBefore(end)) {
                throw new AppException(ErrorCode.SHOW_INVALID_TIME);
            }

            if (!saleStart.isBefore(start)) {
                throw new AppException(ErrorCode.SHOW_INVALID_SALE_START_TIME);
            }

            if (saleEnd != null && saleEnd.isAfter(start)) {
                throw new AppException(ErrorCode.SHOW_INVALID_SALE_END_TIME);
            }

            for (TicketType tt : show.getTicketTypes()) {
                if (tt.getPrice() == null || tt.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new AppException(ErrorCode.TICKET_INVALID_PRICE);
                }

                if (tt.getTotalQuantity() == null || tt.getTotalQuantity() <= 0) {
                    throw new AppException(ErrorCode.TICKET_INVALID_QUANTITY);
                }

                if (tt.getMaxPerOrder() == null || tt.getMaxPerOrder() <= 0
                        || tt.getMaxPerOrder() > tt.getTotalQuantity()) {
                    throw new AppException(ErrorCode.TICKET_INVALID_MAX_PER_ORDER);
                }
            }
        }

        event.setStatus(EventStatus.PENDING_REVIEW);
        Event saved = eventRepository.save(event);

        evictEventDetailCache(eventId);

        return eventMapper.toEventResponse(saved);
    }

    @Override
    public List<EventResponse> getPendingEvents() {
        List<Event> pendingEvents = eventRepository
                .findByStatusOrderByEarliestShowStartTime(EventStatus.PENDING_REVIEW);
        return pendingEvents.stream().map(eventMapper::toEventResponse).toList();
    }

    @Override
    @Transactional
    public EventResponse approveEvent(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (event.getStatus() == EventStatus.PUBLISHED || event.getStatus() == EventStatus.CANCELLED
                || event.getStatus() == EventStatus.COMPLETED) {
            throw new AppException(ErrorCode.EVENT_NOT_DRAFT);
        }

        event.setStatus(EventStatus.PUBLISHED);
        Event saved = eventRepository.save(event);

        evictEventDetailCache(eventId);

        return eventMapper.toEventResponse(saved);
    }

    @Override
    @Transactional
    public EventResponse rejectEvent(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (event.getStatus() != EventStatus.PENDING_REVIEW) {
            throw new AppException(ErrorCode.EVENT_NOT_DRAFT);
        }

        event.setStatus(EventStatus.REJECTED);
        Event saved = eventRepository.save(event);

        evictEventDetailCache(eventId);

        return eventMapper.toEventResponse(saved);
    }

    private void evictEventDetailCache(String eventId) {
        var cache = cacheManager.getCache("eventDetail");
        if (cache != null) {
            cache.evict(eventId);
        }
        var listCache = cacheManager.getCache("eventsList");
        if (listCache != null) {
            listCache.clear();
        }
    }

    @Override
    public List<EventResponse> getEvents() {
        List<EventResponse> allEvents = self.getEventsFromCache();
        LocalDateTime now = LocalDateTime.now();
        return allEvents.stream()
                .filter(e -> e.shows() != null && e.shows().stream()
                        .anyMatch(show -> show.startTime() != null && show.startTime().isAfter(now)))
                .toList();
    }

    @Override
    @Cacheable(value = "eventsList", key = "'published'")
    public List<EventResponse> getEventsFromCache() {
        List<Event> events = eventRepository.findByStatusOrderByEarliestShowStartTime(EventStatus.PUBLISHED);
        return events.stream().map(eventMapper::toEventResponse).toList();
    }

    @Override
    public EventResponse getEventById(String id) {
        EventResponse eventResponse = self.getEventDetailFromCache(id);
        return eventResponse;
    }

    @Override
    @Cacheable(value = "eventDetail", key = "#id")
    public EventResponse getEventDetailFromCache(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));
        return eventMapper.toEventResponse(event);
    }

}