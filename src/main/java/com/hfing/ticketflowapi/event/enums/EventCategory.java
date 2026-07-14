package com.hfing.ticketflowapi.event.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum EventCategory {
    LIVE_MUSIC("Nhạc sống"),
    STAGE_ART("Sân khấu & Nghệ thuật"),
    SPORTS("Thể Thao"),
    WORKSHOP("Hội thảo & Workshop"),
    EXPERIENCE("Tham quan & Trải nghiệm"),
    OTHERS("Khác"),
    RESELL("Vé bán lại"),
    BLOG("Blog");

    private final String displayName;
}