package com.coociks.temfex.service;

import com.coociks.temfex.config.MinioConfig;
import com.coociks.temfex.dto.ShareLinkRequest;
import com.coociks.temfex.dto.ShareLinkResponse;
import com.coociks.temfex.entity.FileEntity;
import com.coociks.temfex.entity.ShareLink;
import com.coociks.temfex.repository.FileRepository;
import com.coociks.temfex.repository.ShareLinkRepository;
import com.coociks.temfex.util.ShortIdGenerator;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareLinkRepository shareLinkRepository;
    private final FileRepository fileRepository;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    // Создание ссылки на файл
    @Transactional
    public ShareLinkResponse createLink(ShareLinkRequest request) {
        FileEntity file = fileRepository.findById(UUID.fromString(request.getFileId()))
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        String shortId = ShortIdGenerator.generate();
        
        // Определяем срок жизни (по умолчанию 24 часа)
        Duration ttl = request.getTtl() != null ? Duration.parse(request.getTtl()) : Duration.ofHours(24);
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(ttl);

        // В реальном проекте пароль нужно хэшировать (например, через BCrypt)
        // Для простоты пока сохраняем как есть, но помечаем, что он есть
        String passwordHash = request.getPassword() != null ? request.getPassword() : null;

        ShareLink link = ShareLink.builder()
                .shortId(shortId)
                .file(file)
                .passwordHash(passwordHash)
                .maxDownloads(request.getMaxDownloads())
                .downloadsCount(0)
                .expiresAt(expiresAt)
                .build();

        shareLinkRepository.save(link);

        return ShareLinkResponse.builder()
                .shortId(shortId)
                .downloadUrl("/s/" + shortId)
                .expiresAt(expiresAt)
                .maxDownloads(request.getMaxDownloads())
                .passwordProtected(passwordHash != null)
                .build();
    }

    // Получение файла по короткой ссылке (с проверками)
    @Transactional
    public InputStream getFileByShortId(String shortId, String password) {
        ShareLink link = shareLinkRepository.findByShortId(shortId)
                .orElseThrow(() -> new RuntimeException("Ссылка не найдена"));

        // 1. Проверка срока действия
        if (link.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Срок действия ссылки истёк");
        }

        // 2. Проверка лимита скачиваний
        if (link.getMaxDownloads() != null && link.getDownloadsCount() >= link.getMaxDownloads()) {
            throw new RuntimeException("Лимит скачиваний исчерпан");
        }

        // 3. Проверка пароля
        if (link.getPasswordHash() != null && !link.getPasswordHash().equals(password)) {
            throw new RuntimeException("Неверный пароль");
        }

        // 4. Увеличиваем счётчик скачиваний
        link.setDownloadsCount(link.getDownloadsCount() + 1);
        shareLinkRepository.save(link);

        // 5. Получаем файл из MinIO
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(link.getFile().getStoredName())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении файла из хранилища: " + e.getMessage(), e);
        }
    }
}