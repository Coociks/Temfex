package com.coociks.temfex.service;

import com.coociks.temfex.entity.FileEntity;
import com.coociks.temfex.entity.ShareLink;
import com.coociks.temfex.repository.FileRepository;
import com.coociks.temfex.repository.ShareLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CleanupService {

    private final ShareLinkRepository shareLinkRepository;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    // Запускается каждые 60 000 мс (1 минута). 
    // В продакшене можно поставить раз в час: fixedRate = 3600000
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredFiles() {
        OffsetDateTime now = OffsetDateTime.now();
        List<ShareLink> expiredLinks = shareLinkRepository.findExpiredLinks(now);

        if (expiredLinks.isEmpty()) {
            return;
        }

        System.out.println(" Найдено просроченных ссылок: " + expiredLinks.size());

        for (ShareLink link : expiredLinks) {
            FileEntity file = link.getFile();
            
            // 1. Удаляем саму ссылку
            shareLinkRepository.delete(link);

            // 2. Проверяем, есть ли у файла другие активные ссылки
            long activeLinksCount = shareLinkRepository.countByFileIdAndExpiresAtAfter(file.getId(), now);
            
            // 3. Если активных ссылок не осталось — удаляем файл из БД и MinIO
            if (activeLinksCount == 0) {
                fileStorageService.deleteFile(file.getStoredName());
                fileRepository.delete(file);
                System.out.println("🗑️ Удалён файл: " + file.getOriginalName());
            }
        }
    }
}