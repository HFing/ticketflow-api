package com.hfing.ticketflowapi.service.impl;

import com.hfing.ticketflowapi.common.enums.EventStatus;
import com.hfing.ticketflowapi.dto.request.CreateEventRequest;
import com.hfing.ticketflowapi.dto.request.UpdateEventRequest;
import com.hfing.ticketflowapi.dto.response.EventResponse;
import com.hfing.ticketflowapi.entity.Event;
import com.hfing.ticketflowapi.entity.EventShow;
import com.hfing.ticketflowapi.entity.User;
import com.hfing.ticketflowapi.exception.ErrorCode;
import com.hfing.ticketflowapi.exception.AppException;
import com.hfing.ticketflowapi.mapper.EventMapper;
import com.hfing.ticketflowapi.repository.EventRepository;
import com.hfing.ticketflowapi.repository.UserRepository;
import com.hfing.ticketflowapi.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    @Autowired
    @Lazy
    private EventService self; // Self-injection to enable proxy invocation for @Cacheable

    @Override
    @Transactional
    @CacheEvict(value = "eventsList", allEntries = true)
    public EventResponse createEvent(CreateEventRequest request, String currentUserId) {
        // Validate startTime is not in the past
        if (request.startTime().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.EVENT_CREATION_PAST_START);
        }

        // Validate startTime is before endTime
        if (!request.startTime().isBefore(request.endTime())) {
            throw new AppException(ErrorCode.EVENT_START_AFTER_END);
        }

        User organizer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Event event = eventMapper.toEvent(request);
        event.setStatus(EventStatus.DRAFT);
        event.setOrganizer(organizer);

        // Add shows
        if (request.shows() != null && !request.shows().isEmpty()) {
            java.util.List<EventShow> shows = request.shows().stream()
                    .map(showRequest -> EventShow.builder()
                            .startTime(showRequest.startTime())
                            .endTime(showRequest.endTime())
                            .event(event)
                            .build())
                    .collect(java.util.stream.Collectors.toList());
            event.setShows(shows);
        }

        // Derive overall start/end times from shows if present
        if (event.getShows() != null && !event.getShows().isEmpty()) {
            LocalDateTime minStart = event.getShows().stream()
                    .map(EventShow::getStartTime)
                    .min(LocalDateTime::compareTo)
                    .orElse(request.startTime());
            LocalDateTime maxEnd = event.getShows().stream()
                    .map(EventShow::getEndTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(request.endTime());
            event.setStartTime(minStart);
            event.setEndTime(maxEnd);
        }

        Event savedEvent = eventRepository.save(event);
        return eventMapper.toEventResponse(savedEvent);
    }

    @Override
    @Cacheable(value = "eventsList", key = "#role + '_' + (#userId != null ? #userId : 'guest')")
    public List<EventResponse> getEvents(String userId, String role) {
        List<Event> events;
        if ("ADMIN".equals(role)) {
            events = eventRepository.findAllByOrderByStartTimeAsc();
        } else if ("ORGANIZER".equals(role)) {
            events = eventRepository.findPublishedOrOrganizerEvents(userId);
        } else {
            events = eventRepository.findByStatusOrderByStartTimeAsc(EventStatus.PUBLISHED);
        }
        return events.stream().map(eventMapper::toEventResponse).toList();
    }

    @Override
    public EventResponse getEventById(String id, String currentUserId, String role) {
        // Invoke through self to utilize cache
        EventResponse eventResponse = self.getEventDetailFromCache(id);

        // Dynamic Role-based validation
        if ("ADMIN".equals(role)) {
            return eventResponse;
        }
        if ("ORGANIZER".equals(role) && eventResponse.organizerId().equals(currentUserId)) {
            return eventResponse;
        }
        if (eventResponse.status() == EventStatus.PUBLISHED) {
            return eventResponse;
        }
        throw new AppException(ErrorCode.EVENT_NOT_FOUND);
    }

    @Override
    @Cacheable(value = "eventDetail", key = "#id")
    public EventResponse getEventDetailFromCache(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));
        return eventMapper.toEventResponse(event);
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

        // If startTime is changed, validate it is in the future
        if (!event.getStartTime().equals(request.startTime()) && request.startTime().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.EVENT_CREATION_PAST_START);
        }

        // Validate startTime is before endTime
        if (!request.startTime().isBefore(request.endTime())) {
            throw new AppException(ErrorCode.EVENT_START_AFTER_END);
        }

        eventMapper.updateEventFromRequest(request, event);

        // Update shows
        event.getShows().clear();
        if (request.shows() != null && !request.shows().isEmpty()) {
            java.util.List<EventShow> shows = request.shows().stream()
                    .map(showRequest -> EventShow.builder()
                            .startTime(showRequest.startTime())
                            .endTime(showRequest.endTime())
                            .event(event)
                            .build())
                    .collect(java.util.stream.Collectors.toList());
            event.getShows().addAll(shows);
        }

        // Derive overall start/end times from shows if present
        if (event.getShows() != null && !event.getShows().isEmpty()) {
            LocalDateTime minStart = event.getShows().stream()
                    .map(EventShow::getStartTime)
                    .min(LocalDateTime::compareTo)
                    .orElse(request.startTime());
            LocalDateTime maxEnd = event.getShows().stream()
                    .map(EventShow::getEndTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(request.endTime());
            event.setStartTime(minStart);
            event.setEndTime(maxEnd);
        }

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
    public EventResponse publishEvent(String id, String currentUserId, String role) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        authorizeModification(event, currentUserId, role);

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new AppException(ErrorCode.EVENT_NOT_DRAFT);
        }

        // Validate required fields
        if (event.getDescription() == null || event.getDescription().isBlank() ||
                event.getLocation() == null || event.getLocation().isBlank() ||
                event.getBannerUrl() == null || event.getBannerUrl().isBlank() ||
                event.getShortImageUrl() == null || event.getShortImageUrl().isBlank() ||
                event.getCategory() == null ||
                event.getShows() == null || event.getShows().isEmpty()) {
            throw new AppException(ErrorCode.EVENT_PUBLISH_MISSING_INFO);
        }

        event.setStatus(EventStatus.PUBLISHED);
        Event publishedEvent = eventRepository.save(event);
        return eventMapper.toEventResponse(publishedEvent);
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
}
