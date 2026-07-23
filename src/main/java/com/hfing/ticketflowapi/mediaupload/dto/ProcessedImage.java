package com.hfing.ticketflowapi.mediaupload.dto;

public record ProcessedImage(
        byte[] content,
        String contentType,
        String extension,
        int width,
        int height) {
}
