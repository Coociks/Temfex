package com.coociks.temfex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private UUID fileId;
    private String originalName;
    private String mimeType;
    private Long size;
    private String message;
}