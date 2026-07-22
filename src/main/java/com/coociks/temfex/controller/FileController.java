package com.coociks.temfex.controller;

import com.coociks.temfex.dto.FileUploadResponse;
import com.coociks.temfex.entity.FileEntity;
import com.coociks.temfex.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                FileUploadResponse.builder()
                    .message("Файл пустой")
                    .build()
            );
        }

        FileEntity savedFile = fileStorageService.uploadFile(file);

        FileUploadResponse response = FileUploadResponse.builder()
                .fileId(savedFile.getId())
                .originalName(savedFile.getOriginalName())
                .mimeType(savedFile.getMimeType())
                .size(savedFile.getSize())
                .message("Файл успешно загружен")
                .build();

        return ResponseEntity.ok(response);
    }
}