package com.coociks.temfex.service;

import com.coociks.temfex.config.MinioConfig;
import com.coociks.temfex.entity.FileEntity;
import com.coociks.temfex.exception.FileUploadException;
import com.coociks.temfex.repository.FileRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final FileRepository fileRepository;

    // 1. Загрузка одного файла
    public FileEntity uploadFile(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : "";
            String storedFilename = UUID.randomUUID().toString() + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storedFilename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            FileEntity fileEntity = FileEntity.builder()
                    .originalName(originalFilename)
                    .storedName(storedFilename)
                    .mimeType(file.getContentType())
                    .size(file.getSize())
                    .build();

            return fileRepository.save(fileEntity);

        } catch (Exception e) {
            throw new FileUploadException("Ошибка при загрузке файла в MinIO: " + e.getMessage());
        }
    }

    // 2. Удаление файла из MinIO
    public void deleteFile(String storedName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storedName)
                            .build()
            );
        } catch (Exception e) {
            throw new FileUploadException("Ошибка при удалении файла из MinIO: " + e.getMessage());
        }
    }

    // 3. Загрузка нескольких файлов (архивация на лету)
    public FileEntity uploadMultipleFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new FileUploadException("Список файлов пуст");
        }
        
        // Если файл один, используем стандартную логику
        if (files.size() == 1) {
            return uploadFile(files.get(0));
        }

        try {
            String zipName = "archive_" + UUID.randomUUID().toString().substring(0, 8) + ".zip";
            String storedName = UUID.randomUUID().toString() + ".zip";

            // Создаем ZIP-архив в оперативной памяти
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        zos.putNextEntry(new ZipEntry(file.getOriginalFilename()));
                        zos.write(file.getBytes());
                        zos.closeEntry();
                    }
                }
            }
            
            byte[] zipBytes = baos.toByteArray();

            // Загружаем байтовый массив в MinIO как поток
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(storedName)
                            .stream(new ByteArrayInputStream(zipBytes), zipBytes.length, -1)
                            .contentType("application/zip")
                            .build()
            );

            FileEntity fileEntity = FileEntity.builder()
                    .originalName(zipName)
                    .storedName(storedName)
                    .mimeType("application/zip")
                    .size((long) zipBytes.length)
                    .build();

            return fileRepository.save(fileEntity);

        } catch (IOException e) {
            throw new FileUploadException("Ошибка при создании ZIP-архива: " + e.getMessage());
        } catch (Exception e) {
            throw new FileUploadException("Ошибка при загрузке архива в MinIO: " + e.getMessage());
        }
    }
}