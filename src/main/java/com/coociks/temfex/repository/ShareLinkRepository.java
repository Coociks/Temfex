package com.coociks.temfex.repository;

import com.coociks.temfex.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {
    Optional<ShareLink> findByShortId(String shortId);

    // Ищем все ссылки, срок которых истёк
    @Query("SELECT l FROM ShareLink l WHERE l.expiresAt < :now")
    List<ShareLink> findExpiredLinks(@Param("now") OffsetDateTime now);

    // Считаем активные ссылки для файла (чтобы не удалить файл, если на него есть другие живые ссылки)
    long countByFileIdAndExpiresAtAfter(UUID fileId, OffsetDateTime now);
}