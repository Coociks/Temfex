package com.coociks.temfex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLinkResponse {
    private String shortId;
    private String downloadUrl;
    private OffsetDateTime expiresAt;
    private Integer maxDownloads;
    private boolean passwordProtected;
}