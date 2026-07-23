package com.hfing.ticketflowapi.mediaupload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.mediaupload.dto.ProcessedImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageProcessorTest {

    private final ImageProcessor imageProcessor = new ImageProcessor();

    @Test
    void processAvatar_acceptsExactSquareDimensionsWithoutChangingContent() throws Exception {
        MockMultipartFile file = image("avatar.png", "image/png", 512, 512, "png");

        ProcessedImage result = imageProcessor.processAvatar(file);

        assertThat(result.content()).isEqualTo(file.getBytes());
        assertThat(result.width()).isEqualTo(ImageProcessor.AVATAR_OUTPUT_SIZE);
        assertThat(result.height()).isEqualTo(ImageProcessor.AVATAR_OUTPUT_SIZE);
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.extension()).isEqualTo("png");
    }

    @Test
    void processAvatar_rejectsImageThatIsNot512By512() throws Exception {
        MockMultipartFile file = image("avatar.png", "image/png", 800, 600, "png");

        assertThatThrownBy(() -> imageProcessor.processAvatar(file))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.IMAGE_DIMENSIONS_INVALID))
                .hasMessageContaining("512x512")
                .hasMessageContaining("800x600");
    }

    @Test
    void validateShortEventImage_acceptsExactDimensions() throws Exception {
        MockMultipartFile file = image(
                "short.jpg",
                "image/jpeg",
                ImageProcessor.SHORT_IMAGE_WIDTH,
                ImageProcessor.SHORT_IMAGE_HEIGHT,
                "jpg");

        ProcessedImage result = imageProcessor.validateShortEventImage(file);

        assertThat(result.width()).isEqualTo(720);
        assertThat(result.height()).isEqualTo(958);
        assertThat(result.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void validateBannerEventImage_rejectsWrongDimensions() throws Exception {
        MockMultipartFile file = image("banner.png", "image/png", 1280, 719, "png");

        assertThatThrownBy(() -> imageProcessor.validateBannerEventImage(file))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.IMAGE_DIMENSIONS_INVALID))
                .hasMessageContaining("1280x720")
                .hasMessageContaining("1280x719");
    }

    @Test
    void imageWithForgedContentTypeIsRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "this is not an image".getBytes());

        assertThatThrownBy(() -> imageProcessor.processAvatar(file))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.IMAGE_FORMAT_INVALID));
    }

    @Test
    void imageWithMismatchedMimeTypeIsRejected() throws Exception {
        MockMultipartFile file = image("avatar.png", "image/jpeg", 512, 512, "png");

        assertThatThrownBy(() -> imageProcessor.processAvatar(file))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.IMAGE_FORMAT_INVALID));
    }

    private MockMultipartFile image(
            String fileName,
            String contentType,
            int width,
            int height,
            String format) throws Exception {
        int type = format.equals("png")
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;
        BufferedImage image = new BufferedImage(width, height, type);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, output);
            return new MockMultipartFile("file", fileName, contentType, output.toByteArray());
        }
    }
}
