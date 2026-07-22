package com.coociks.temfex.dto;

import lombok.Data;

@Data
public class ShareLinkRequest {
    // ID файла, для которого создаём ссылку
    private String fileId;
    
    // Срок жизни ссылки (например, "PT24H" для 24 часов, "PT1H" для 1 часа)
    // Если не указано, ссылка будет жить 24 часа
    private String ttl; 
    
    // Максимальное количество скачиваний (0 = без ограничений)
    private Integer maxDownloads;
    
    // Пароль для скачивания (опционально)
    private String password;
}