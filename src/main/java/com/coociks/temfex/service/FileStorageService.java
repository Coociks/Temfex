package com.coociks.temfex.service;

import com.coociks.temfex.config.MinioConfig;
import com.coociks.temfex.entity.FileEntity;
import com.coociks.temfex.repository.FileRepository;
import io.minio.PutObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import io.minio.RemoveObjectArgs;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final FileRepository fileRepository;

    public FileEntity uploadFile(MultipartFile file) {
        try {
            // 1. Генерируем уникальное имя для хранения (чтобы файлы с одинаковыми именами не перезаписывались)
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : "";
            String storedFilename = UUID.randomUUID().toString() + extension;

            // 2. Загружаем файл в MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storedFilename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 3. Сохраняем метаданные в PostgreSQL
            FileEntity fileEntity = FileEntity.builder()
                    .originalName(originalFilename)
                    .storedName(storedFilename)
                    .mimeType(file.getContentType())
                    .size(file.getSize())
                    .build();

            return fileRepository.save(fileEntity);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке файла в MinIO: " + e.getMessage(), e);
        }
    }
    public void deleteFile(String storedName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storedName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении файла из MinIO: " + e.getMessage(), e);
        }
    }
}