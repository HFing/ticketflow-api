package com.hfing.ticketflowapi.mediaupload.service.impl;

import com.hfing.ticketflowapi.common.exception.AppException;
import com.hfing.ticketflowapi.common.exception.ErrorCode;
import com.hfing.ticketflowapi.mediaupload.dto.response.FileResponse;
import com.hfing.ticketflowapi.mediaupload.service.IStorageService;
import java.net.URL;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "S3-STORAGE")
public class S3StorageService implements IStorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Override
    public FileResponse upload(
            byte[] content,
            String contentType,
            String extension,
            String folder) {
        String normalizedFolder = normalizeFolder(folder);
        String fileName = UUID.randomUUID() + "." + extension;
        String key = normalizedFolder + "/" + fileName;

        putObject(key, content, contentType);

        return FileResponse.builder()
                .key(key)
                .fileName(fileName)
                .contentType(contentType)
                .size(content.length)
                .url(getUrl(key))
                .build();
    }

    @Override
    public void deleteFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build());
        } catch (S3Exception exception) {
            // Updating the database must not be rolled back only because cleanup failed.
            log.warn("Could not delete old S3 object with key {}", fileKey, exception);
        }
    }

    @Override
    public String getUrl(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return null;
        }
        URL url = s3Client.utilities().getUrl(GetUrlRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build());
        return url.toExternalForm();
    }

    private void putObject(String key, byte[] content, String contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .contentLength((long) content.length)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (S3Exception exception) {
            throw storageException(exception);
        }
    }

    private String normalizeFolder(String folder) {
        if (folder == null
                || !folder.matches("[a-zA-Z0-9/_-]+")
                || folder.startsWith("/")
                || folder.endsWith("/")
                || folder.contains("..")) {
            throw new IllegalArgumentException("Invalid S3 folder");
        }
        return folder;
    }

    private AppException storageException(S3Exception exception) {
        log.error("S3 operation failed for bucket {}", bucketName, exception);
        return new AppException(ErrorCode.STORAGE_OPERATION_FAILED);
    }
}
