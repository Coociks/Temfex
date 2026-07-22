package com.coociks.temfex.controller;

import com.coociks.temfex.dto.ShareLinkRequest;
import com.coociks.temfex.dto.ShareLinkResponse;
import com.coociks.temfex.entity.FileEntity;
import com.coociks.temfex.entity.ShareLink;
import com.coociks.temfex.repository.ShareLinkRepository;
import com.coociks.temfex.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

@Controller
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;
    private final ShareLinkRepository shareLinkRepository;

    @PostMapping("/api/links")
    @ResponseBody
    public ResponseEntity<ShareLinkResponse> createLink(@RequestBody ShareLinkRequest request) {
        ShareLinkResponse response = shareService.createLink(request);
        return ResponseEntity.ok(response);
    }

    @Transactional(readOnly = true)
    @GetMapping("/s/{shortId}")
    public String showDownloadPage(@PathVariable String shortId, Model model) {
        try {
            ShareLink link = shareLinkRepository.findByShortId(shortId)
                    .orElseThrow(() -> new RuntimeException("Ссылка не найдена или истекла"));
            
            FileEntity file = link.getFile(); // Теперь это сработает благодаря @Transactional
            
            model.addAttribute("originalName", file.getOriginalName());
            model.addAttribute("mimeType", file.getMimeType());
            model.addAttribute("size", formatFileSize(file.getSize()));
            model.addAttribute("shortId", shortId);
            model.addAttribute("passwordRequired", link.getPasswordHash() != null);
            
            return "download-page";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error-page";
        }
    }

    @Transactional
    @GetMapping("/s/{shortId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable String shortId,
            @RequestParam(required = false) String password) {
        
        System.out.println("📥 Запрос на скачивание shortId: " + shortId); // Для отладки
        
        ShareLink link = shareLinkRepository.findByShortId(shortId)
                .orElseThrow(() -> new RuntimeException("Ссылка не найдена или истекла"));
        
        FileEntity file = link.getFile();
        InputStream inputStream = shareService.getFileByShortId(shortId, password);

        String encodedFilename = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(new InputStreamResource(inputStream));
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " Б";
        else if (bytes < 1024 * 1024) return new DecimalFormat("#.##").format(bytes / 1024.0) + " КБ";
        else if (bytes < 1024 * 1024 * 1024) return new DecimalFormat("#.##").format(bytes / (1024.0 * 1024.0)) + " МБ";
        else return new DecimalFormat("#.##").format(bytes / (1024.0 * 1024.0 * 1024.0)) + " ГБ";
    }
}