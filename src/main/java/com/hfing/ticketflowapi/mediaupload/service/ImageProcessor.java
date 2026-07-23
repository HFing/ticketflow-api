package com.hfing.ticketflowapi.mediaupload.service;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.mediaupload.dto.ProcessedImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageProcessor {

    public static final long AVATAR_MAX_BYTES = 5L * 1024 * 1024;
    public static final long EVENT_IMAGE_MAX_BYTES = 10L * 1024 * 1024;
    public static final int AVATAR_OUTPUT_SIZE = 512;
    public static final int SHORT_IMAGE_WIDTH = 720;
    public static final int SHORT_IMAGE_HEIGHT = 958;
    public static final int BANNER_IMAGE_WIDTH = 1280;
    public static final int BANNER_IMAGE_HEIGHT = 720;
    private static final int MAX_DECODE_DIMENSION = 8_000;

    public ProcessedImage processAvatar(MultipartFile file) {
        DecodedImage decoded = decode(file, AVATAR_MAX_BYTES);
        if (decoded.width() != AVATAR_OUTPUT_SIZE || decoded.height() != AVATAR_OUTPUT_SIZE) {
            throw new AppException(
                    ErrorCode.IMAGE_DIMENSIONS_INVALID,
                    "Avatar must be exactly %dx%d pixels (received %dx%d)"
                            .formatted(
                                    AVATAR_OUTPUT_SIZE,
                                    AVATAR_OUTPUT_SIZE,
                                    decoded.width(),
                                    decoded.height()));
        }
        return new ProcessedImage(
                decoded.originalContent(),
                decoded.format().contentType(),
                decoded.format().extension(),
                AVATAR_OUTPUT_SIZE,
                AVATAR_OUTPUT_SIZE);
    }

    public ProcessedImage validateShortEventImage(MultipartFile file) {
        return validateExactDimensions(file, SHORT_IMAGE_WIDTH, SHORT_IMAGE_HEIGHT, "short event image");
    }

    public ProcessedImage validateBannerEventImage(MultipartFile file) {
        return validateExactDimensions(file, BANNER_IMAGE_WIDTH, BANNER_IMAGE_HEIGHT, "event banner");
    }

    private ProcessedImage validateExactDimensions(
            MultipartFile file,
            int requiredWidth,
            int requiredHeight,
            String imageName) {
        DecodedImage decoded = decode(file, EVENT_IMAGE_MAX_BYTES);
        if (decoded.width() != requiredWidth || decoded.height() != requiredHeight) {
            throw new AppException(
                    ErrorCode.IMAGE_DIMENSIONS_INVALID,
                    "%s must be exactly %dx%d pixels (received %dx%d)"
                            .formatted(
                                    imageName,
                                    requiredWidth,
                                    requiredHeight,
                                    decoded.width(),
                                    decoded.height()));
        }

        return new ProcessedImage(
                decoded.originalContent(),
                decoded.format().contentType(),
                decoded.format().extension(),
                decoded.width(),
                decoded.height());
    }

    private DecodedImage decode(MultipartFile file, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.IMAGE_REQUIRED);
        }
        if (file.getSize() > maxBytes) {
            throw new AppException(
                    ErrorCode.IMAGE_TOO_LARGE,
                    "Image must not exceed %d MB".formatted(maxBytes / 1024 / 1024));
        }

        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > maxBytes) {
                throw new AppException(
                        ErrorCode.IMAGE_TOO_LARGE,
                        "Image must not exceed %d MB".formatted(maxBytes / 1024 / 1024));
            }
            ImageFormat format = detectFormat(bytes);
            validateDeclaredContentType(file.getContentType(), format);

            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                if (input == null) {
                    throw new AppException(ErrorCode.IMAGE_FORMAT_INVALID);
                }

                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) {
                    throw new AppException(ErrorCode.IMAGE_FORMAT_INVALID);
                }

                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width <= 0
                            || height <= 0
                            || width > MAX_DECODE_DIMENSION
                            || height > MAX_DECODE_DIMENSION) {
                        throw new AppException(
                                ErrorCode.IMAGE_DIMENSIONS_INVALID,
                                "Image dimensions must be between 1 and %d pixels"
                                        .formatted(MAX_DECODE_DIMENSION));
                    }

                    BufferedImage image = reader.read(0);
                    if (image == null) {
                        throw new AppException(ErrorCode.IMAGE_FORMAT_INVALID);
                    }
                    return new DecodedImage(bytes, image, format, width, height);
                } finally {
                    reader.dispose();
                }
            }
        } catch (AppException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new AppException(ErrorCode.IMAGE_PROCESSING_FAILED);
        }
    }

    private ImageFormat detectFormat(byte[] bytes) {
        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4e
                && bytes[3] == 0x47
                && bytes[4] == 0x0d
                && bytes[5] == 0x0a
                && bytes[6] == 0x1a
                && bytes[7] == 0x0a) {
            return ImageFormat.PNG;
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return ImageFormat.JPEG;
        }
        throw new AppException(ErrorCode.IMAGE_FORMAT_INVALID);
    }

    private void validateDeclaredContentType(String declaredContentType, ImageFormat format) {
        if (declaredContentType == null || declaredContentType.isBlank()) {
            throw new AppException(ErrorCode.IMAGE_FORMAT_INVALID);
        }
        String normalized = declaredContentType.equalsIgnoreCase("image/jpg")
                ? "image/jpeg"
                : declaredContentType.toLowerCase();
        if (!format.contentType().equals(normalized)) {
            throw new AppException(ErrorCode.IMAGE_FORMAT_INVALID);
        }
    }

    private enum ImageFormat {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png");

        private final String contentType;
        private final String extension;

        ImageFormat(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        String contentType() {
            return contentType;
        }

        String extension() {
            return extension;
        }

    }

    private record DecodedImage(
            byte[] originalContent,
            BufferedImage image,
            ImageFormat format,
            int width,
            int height) {
    }
}
