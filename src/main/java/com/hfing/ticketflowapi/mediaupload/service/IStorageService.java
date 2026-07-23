package com.hfing.ticketflowapi.mediaupload.service;

import com.hfing.ticketflowapi.mediaupload.dto.response.FileResponse;

public interface IStorageService {
    FileResponse upload(
            byte[] content,
            String contentType,
            String extension,
            String folder);

    void deleteFile(String fileKey);

    String getUrl(String fileKey);
}
