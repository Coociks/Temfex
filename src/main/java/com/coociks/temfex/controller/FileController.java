package com.coociks.temfex.controller;

import com.coociks.temfex.dto.FileUploadResponse;
import com.coociks.temfex.entity.FileEntity;
import com.coociks.temfex.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFiles(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(
                FileUploadResponse.builder().message("Файлы не выбраны").build()
            );
        }

        FileEntity savedFile = fileStorageService.uploadMultipleFiles(files);

        String message = files.size() == 1 ? "Файл успешно загружен" : "Архив успешно создан и загружен";

        FileUploadResponse response = FileUploadResponse.builder()
                .fileId(savedFile.getId())
                .originalName(savedFile.getOriginalName())
                .mimeType(savedFile.getMimeType())
                .size(savedFile.getSize())
                .message(message)
                .build();

        return ResponseEntity.ok(response);
    }
}