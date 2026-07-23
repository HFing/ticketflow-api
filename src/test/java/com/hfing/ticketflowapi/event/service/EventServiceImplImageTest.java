package com.hfing.ticketflowapi.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.event.dto.request.CreateEventRequest;
import com.hfing.ticketflowapi.event.dto.response.EventResponse;
import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.enums.EventCategory;
import com.hfing.ticketflowapi.event.enums.EventStatus;
import com.hfing.ticketflowapi.event.mapper.EventMapper;
import com.hfing.ticketflowapi.event.mapper.EventShowMapper;
import com.hfing.ticketflowapi.event.mapper.TicketTypeMapper;
import com.hfing.ticketflowapi.event.repository.EventRepository;
import com.hfing.ticketflowapi.event.repository.EventShowRepository;
import com.hfing.ticketflowapi.event.repository.TicketTypeRepository;
import com.hfing.ticketflowapi.event.service.impl.EventServiceImpl;
import com.hfing.ticketflowapi.mediaupload.dto.ProcessedImage;
import com.hfing.ticketflowapi.mediaupload.dto.response.FileResponse;
import com.hfing.ticketflowapi.mediaupload.service.IStorageService;
import com.hfing.ticketflowapi.mediaupload.service.ImageProcessor;
import com.hfing.ticketflowapi.user.entity.User;
import com.hfing.ticketflowapi.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class EventServiceImplImageTest {

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventMapper eventMapper;
    @Mock private EventShowRepository eventShowRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private EventShowMapper eventShowMapper;
    @Mock private TicketTypeMapper ticketTypeMapper;
    @Mock private CacheManager cacheManager;
    @Mock private ImageProcessor imageProcessor;
    @Mock private IStorageService storageService;

    @InjectMocks
    private EventServiceImpl eventService;

    private MockMultipartFile shortImage;
    private MockMultipartFile bannerImage;
    private ProcessedImage validatedShortImage;
    private ProcessedImage validatedBannerImage;
    private CreateEventRequest request;
    private Event event;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("organizer-1")
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(jwt, null));

        shortImage = new MockMultipartFile(
                "shortImage", "short.png", "image/png", new byte[] {1});
        bannerImage = new MockMultipartFile(
                "bannerImage", "banner.jpg", "image/jpeg", new byte[] {2});
        validatedShortImage = new ProcessedImage(
                new byte[] {1}, "image/png", "png", 720, 958);
        validatedBannerImage = new ProcessedImage(
                new byte[] {2}, "image/jpeg", "jpg", 1280, 720);
        request = new CreateEventRequest(
                "Concert",
                "Description",
                "Ho Chi Minh City",
                "TicketFlow Hall",
                EventCategory.LIVE_MUSIC);
        event = Event.builder().build();

        when(imageProcessor.validateShortEventImage(shortImage))
                .thenReturn(validatedShortImage);
        when(imageProcessor.validateBannerEventImage(bannerImage))
                .thenReturn(validatedBannerImage);
        when(userRepository.findById("organizer-1"))
                .thenReturn(Optional.of(User.builder().id("organizer-1").build()));
        when(eventMapper.toEvent(request)).thenReturn(event);
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("event-1");
            }
            return saved;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createEvent_uploadsBothImagesInSingleOperation() {
        FileResponse uploadedShort = file(
                "events/event-1/short/short.png",
                "https://cdn/short.png");
        FileResponse uploadedBanner = file(
                "events/event-1/banner/banner.jpg",
                "https://cdn/banner.jpg");
        EventResponse expectedResponse = org.mockito.Mockito.mock(EventResponse.class);

        when(storageService.upload(any(), anyString(), anyString(), contains("/short")))
                .thenReturn(uploadedShort);
        when(storageService.upload(any(), anyString(), anyString(), contains("/banner")))
                .thenReturn(uploadedBanner);
        when(eventMapper.toEventResponse(event)).thenReturn(expectedResponse);

        EventResponse response = eventService.createEvent(request, shortImage, bannerImage);

        assertThat(response).isSameAs(expectedResponse);
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(event.getShortImageKey()).isEqualTo(uploadedShort.key());
        assertThat(event.getShortImageUrl()).isEqualTo(uploadedShort.url());
        assertThat(event.getBannerKey()).isEqualTo(uploadedBanner.key());
        assertThat(event.getBannerUrl()).isEqualTo(uploadedBanner.url());
        verify(storageService).upload(
                validatedShortImage.content(),
                "image/png",
                "png",
                "events/event-1/short");
        verify(storageService).upload(
                validatedBannerImage.content(),
                "image/jpeg",
                "jpg",
                "events/event-1/banner");
    }

    @Test
    void createEvent_whenBannerUploadFails_deletesUploadedShortImage() {
        FileResponse uploadedShort = file(
                "events/event-1/short/short.png",
                "https://cdn/short.png");
        when(storageService.upload(any(), anyString(), anyString(), contains("/short")))
                .thenReturn(uploadedShort);
        when(storageService.upload(any(), anyString(), anyString(), contains("/banner")))
                .thenThrow(new AppException(ErrorCode.STORAGE_OPERATION_FAILED));

        assertThatThrownBy(() -> eventService.createEvent(request, shortImage, bannerImage))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.STORAGE_OPERATION_FAILED.getMessage());

        verify(storageService).deleteFile(uploadedShort.key());
        verify(eventMapper, never()).toEventResponse(any());
    }

    private FileResponse file(String key, String url) {
        return FileResponse.builder()
                .key(key)
                .fileName(key.substring(key.lastIndexOf('/') + 1))
                .contentType(key.endsWith(".png") ? "image/png" : "image/jpeg")
                .size(1)
                .url(url)
                .build();
    }
}
