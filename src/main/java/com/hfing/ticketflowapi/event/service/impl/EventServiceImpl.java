package com.hfing.ticketflowapi.event.service.impl;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.event.dto.request.CreateEventRequest;
import com.hfing.ticketflowapi.event.dto.request.CreateEventShowRequest;
import com.hfing.ticketflowapi.event.dto.request.CreateTicketTypeRequest;
import com.hfing.ticketflowapi.event.dto.request.UpdateEventRequest;
import com.hfing.ticketflowapi.event.dto.response.EventResponse;
import com.hfing.ticketflowapi.event.dto.response.EventShowResponse;
import com.hfing.ticketflowapi.event.dto.response.PublicEventResponse;
import com.hfing.ticketflowapi.event.dto.response.PublicEventShowResponse;
import com.hfing.ticketflowapi.event.dto.response.PublicEventSummaryResponse;
import com.hfing.ticketflowapi.event.dto.response.TicketTypeResponse;
import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.entity.EventShow;
import com.hfing.ticketflowapi.event.entity.TicketType;
import com.hfing.ticketflowapi.event.enums.EventShowSaleStatus;
import com.hfing.ticketflowapi.event.enums.EventShowStatus;
import com.hfing.ticketflowapi.event.enums.EventStatus;
import com.hfing.ticketflowapi.event.enums.TicketTypeStatus;
import com.hfing.ticketflowapi.event.mapper.EventMapper;
import com.hfing.ticketflowapi.event.mapper.EventShowMapper;
import com.hfing.ticketflowapi.event.mapper.TicketTypeMapper;
import com.hfing.ticketflowapi.event.repository.EventRepository;
import com.hfing.ticketflowapi.event.repository.EventShowRepository;
import com.hfing.ticketflowapi.event.repository.TicketTypeRepository;
import com.hfing.ticketflowapi.event.service.IEventService;
import com.hfing.ticketflowapi.mediaupload.dto.ProcessedImage;
import com.hfing.ticketflowapi.mediaupload.dto.response.FileResponse;
import com.hfing.ticketflowapi.mediaupload.service.IStorageService;
import com.hfing.ticketflowapi.mediaupload.service.ImageProcessor;
import com.hfing.ticketflowapi.user.entity.User;
import com.hfing.ticketflowapi.user.enums.RoleType;
import com.hfing.ticketflowapi.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements IEventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final EventShowRepository eventShowRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final EventShowMapper eventShowMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final CacheManager cacheManager;
    private final ImageProcessor imageProcessor;
    private final IStorageService storageService;

    @Override
    @Transactional
    @CacheEvict(value = "eventsList", allEntries = true)
    public EventResponse createEvent(
            CreateEventRequest request,
            MultipartFile shortImage,
            MultipartFile bannerImage) {

        // Reject invalid images before creating anything in the database or S3.
        ProcessedImage validatedShortImage = imageProcessor.validateShortEventImage(shortImage);
        ProcessedImage validatedBannerImage = imageProcessor.validateBannerEventImage(bannerImage);

        Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        User organizer = userRepository.findById(jwt.getSubject())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Event event = eventMapper.toEvent(request);
        event.setStatus(EventStatus.DRAFT);
        event.setOrganizer(organizer);

        Event savedEvent = eventRepository.saveAndFlush(event);
        UploadedEventImages uploadedImages = uploadEventImages(
                savedEvent.getId(),
                validatedShortImage,
                validatedBannerImage);

        try {
            applyUploadedImages(savedEvent, uploadedImages);
            eventRepository.saveAndFlush(savedEvent);
        } catch (RuntimeException exception) {
            deleteUploadedImages(uploadedImages);
            throw exception;
        }

        return eventMapper.toEventResponse(savedEvent);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "eventsList", allEntries = true),
            @CacheEvict(value = "adminEventDetail", key = "#id"),
            @CacheEvict(value = "publicEventDetail", key = "#id")
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
            @CacheEvict(value = "adminEventDetail", key = "#id"),
            @CacheEvict(value = "publicEventDetail", key = "#id")
    })
    public void deleteEvent(String id, String currentUserId, String role) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        authorizeModification(event, currentUserId, role);

        eventRepository.delete(event);
        storageService.deleteFile(event.getShortImageKey());
        storageService.deleteFile(event.getBannerKey());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "eventsList", allEntries = true),
            @CacheEvict(value = "adminEventDetail", key = "#id"),
            @CacheEvict(value = "publicEventDetail", key = "#id")
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
        if (RoleType.ADMIN.name().equals(role)) {
            return;
        }
        if (RoleType.ORGANIZER.name().equals(role)) {
            if (event.getOrganizer() == null || !event.getOrganizer().getId().equals(currentUserId)) {
                throw new AppException(ErrorCode.EVENT_FORBIDDEN_MODIFICATION);
            }
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN);
    }

    private UploadedEventImages uploadEventImages(
            String eventId,
            ProcessedImage shortImage,
            ProcessedImage bannerImage) {
        FileResponse uploadedShortImage = storageService.upload(
                shortImage.content(),
                shortImage.contentType(),
                shortImage.extension(),
                "events/" + eventId + "/short");

        try {
            FileResponse uploadedBannerImage = storageService.upload(
                    bannerImage.content(),
                    bannerImage.contentType(),
                    bannerImage.extension(),
                    "events/" + eventId + "/banner");
            return new UploadedEventImages(uploadedShortImage, uploadedBannerImage);
        } catch (RuntimeException exception) {
            storageService.deleteFile(uploadedShortImage.key());
            throw exception;
        }
    }

    private void applyUploadedImages(Event event, UploadedEventImages images) {
        event.setShortImageKey(images.shortImage().key());
        event.setShortImageUrl(images.shortImage().url());
        event.setBannerKey(images.bannerImage().key());
        event.setBannerUrl(images.bannerImage().url());
    }

    private void deleteUploadedImages(UploadedEventImages images) {
        storageService.deleteFile(images.shortImage().key());
        storageService.deleteFile(images.bannerImage().key());
    }

    private record UploadedEventImages(
            FileResponse shortImage,
            FileResponse bannerImage) {
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
                .status(EventShowStatus.COMING_SOON)
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
        if (event.getShortImageUrl() == null
                || event.getShortImageUrl().isBlank()
                || event.getBannerUrl() == null
                || event.getBannerUrl().isBlank()) {
            throw new AppException(ErrorCode.EVENT_IMAGES_REQUIRED);
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
    public Page<EventResponse> getPendingEvents(Pageable pageable) {
        return eventRepository
                .findByStatusOrderByEarliestShowStartTime(EventStatus.PENDING_REVIEW, pageable)
                .map(eventMapper::toEventResponse);
    }

    @Override
    @Transactional
    public EventResponse approveEvent(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (event.getStatus() != EventStatus.PENDING_REVIEW) {
            throw new AppException(ErrorCode.EVENT_NOT_PENDING_REVIEW);
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
            throw new AppException(ErrorCode.EVENT_NOT_PENDING_REVIEW);
        }

        event.setStatus(EventStatus.REJECTED);
        Event saved = eventRepository.save(event);

        evictEventDetailCache(eventId);

        return eventMapper.toEventResponse(saved);
    }

    @Override
    @Transactional
    public EventResponse setHotEvent(String eventId, boolean isHot) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        event.setIsHot(isHot);
        Event saved = eventRepository.save(event);

        evictEventDetailCache(eventId);

        return eventMapper.toEventResponse(saved);
    }

    private void evictEventDetailCache(String eventId) {
        var adminDetailCache = cacheManager.getCache("adminEventDetail");
        if (adminDetailCache != null) {
            adminDetailCache.evict(eventId);
        }
        var publicDetailCache = cacheManager.getCache("publicEventDetail");
        if (publicDetailCache != null) {
            publicDetailCache.evict(eventId);
        }
        var listCache = cacheManager.getCache("eventsList");
        if (listCache != null) {
            listCache.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "eventsList", key = "'published-upcoming:' + #pageable")
    public Page<PublicEventSummaryResponse> getPublishedUpcomingEvents(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return eventRepository
                .findPublishedEventsWithUpcomingShows(
                        EventStatus.PUBLISHED,
                        now,
                        pageable)
                .map(event -> toPublicEventSummaryResponse(event, now));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "eventsList", key = "'admin-all:' + #pageable")
    public Page<EventResponse> getAllEventsForAdmin(Pageable pageable) {
        return eventRepository
                .findAllOrderByEarliestShowStartTime(pageable)
                .map(eventMapper::toEventResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "adminEventDetail", key = "#id")
    public EventResponse getAdminEventById(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        return eventMapper.toEventResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "publicEventDetail", key = "#id")
    public PublicEventResponse getPublicEventById(String id) {
        Event event = eventRepository
                .findByIdAndStatusPublished(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        return toPublicEventResponse(event);
    }

    private PublicEventResponse toPublicEventResponse(Event event) {
        return eventMapper.toPublicEventResponse(
                event,
                getOrganizerName(event.getOrganizer()),
                getMinPrice(event),
                event.getShows().stream()
                        .map(this::toPublicEventShowResponse)
                        .toList());
    }

    private PublicEventSummaryResponse toPublicEventSummaryResponse(Event event, LocalDateTime now) {
        return eventMapper.toPublicEventSummaryResponse(
                event,
                getOrganizerName(event.getOrganizer()),
                getMinPrice(event),
                getNearestUpcomingShowDay(event, now));
    }

    private PublicEventShowResponse toPublicEventShowResponse(EventShow show) {
        return eventShowMapper.toPublicEventShowResponse(
                show,
                getSaleStatus(show, LocalDateTime.now()),
                show.getTicketTypes().stream()
                        .map(ticketTypeMapper::toPublicTicketTypeResponse)
                        .toList());
    }

    private BigDecimal getMinPrice(Event event) {
        return event.getShows().stream()
                .flatMap(show -> show.getTicketTypes().stream())
                .filter(ticketType -> ticketType.getStatus() == TicketTypeStatus.ACTIVE)
                .map(TicketType::getPrice)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private Instant getNearestUpcomingShowDay(Event event, LocalDateTime now) {
        return event.getShows().stream()
                .map(EventShow::getStartTime)
                .filter(Objects::nonNull)
                .filter(startTime -> startTime.isAfter(now))
                .min(Comparator.naturalOrder())
                .map(startTime -> startTime.atZone(ZoneId.systemDefault()).toInstant())
                .orElse(null);
    }

    private String getOrganizerName(User organizer) {
        String fullName = List.of(organizer.getFirstName(), organizer.getLastName())
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .reduce((first, second) -> first + " " + second)
                .orElse(null);

        return fullName != null ? fullName : organizer.getEmail();
    }

    private EventShowSaleStatus getSaleStatus(EventShow show, LocalDateTime now) {
        if (show.getStatus() == EventShowStatus.CANCELLED) {
            return EventShowSaleStatus.CANCELLED;
        }
        if (show.getStatus() == EventShowStatus.COMPLETED || now.isAfter(show.getEndTime())) {
            return EventShowSaleStatus.COMPLETED;
        }
        if (now.isBefore(show.getSaleStartTime())) {
            return EventShowSaleStatus.COMING_SOON;
        }
        if (show.getSaleEndTime() != null && now.isAfter(show.getSaleEndTime())) {
            return EventShowSaleStatus.ENDED;
        }
        return EventShowSaleStatus.ON_SALE;
    }

}
