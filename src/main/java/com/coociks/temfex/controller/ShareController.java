package com.coociks.temfex.controller;

import com.coociks.temfex.dto.ShareLinkRequest;
import com.coociks.temfex.dto.ShareLinkResponse;
import com.coociks.temfex.entity.DownloadStat;
import com.coociks.temfex.entity.FileEntity;
import com.coociks.temfex.entity.ShareLink;
import com.coociks.temfex.repository.ShareLinkRepository;
import com.coociks.temfex.service.ShareService;
import com.coociks.temfex.util.QrCodeGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Tag(name = "Share Links", description = "API для работы с временными ссылками на файлы")
public class ShareController {

    private final ShareService shareService;
    private final ShareLinkRepository shareLinkRepository;

    @Operation(
        summary = "Создать ссылку на файл",
        description = "Создаёт временную ссылку с настройками TTL, лимита скачиваний и пароля"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ссылка успешно создана"),
        @ApiResponse(responseCode = "400", description = "Некорректный запрос (файл не найден)")
    })
    @PostMapping("/api/links")
    @ResponseBody
    public ResponseEntity<ShareLinkResponse> createLink(@RequestBody ShareLinkRequest request) {
        ShareLinkResponse response = shareService.createLink(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Страница скачивания файла",
        description = "Показывает информацию о файле, QR-код и кнопку скачивания"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Страница успешно загружена"),
        @ApiResponse(responseCode = "404", description = "Ссылка не найдена или истекла")
    })
    @Transactional(readOnly = true)
    @GetMapping("/s/{shortId}")
    public String showDownloadPage(
            @Parameter(description = "Короткий идентификатор ссылки")
            @PathVariable String shortId,
            Model model) {
        try {
            ShareLink link = shareLinkRepository.findByShortId(shortId)
                    .orElseThrow(() -> new RuntimeException("Ссылка не найдена или истекла"));
            
            FileEntity file = link.getFile();
            
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

    @Operation(
        summary = "Скачать файл по ссылке",
        description = "Скачивает файл. Проверяет срок действия, лимит скачиваний и пароль"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Файл успешно скачан"),
        @ApiResponse(responseCode = "404", description = "Ссылка не найдена"),
        @ApiResponse(responseCode = "403", description = "Превышен лимит скачиваний или неверный пароль")
    })
    @Transactional
    @GetMapping("/s/{shortId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @Parameter(description = "Короткий идентификатор ссылки")
            @PathVariable String shortId,
            @Parameter(description = "Пароль для скачивания (если установлен)")
            @RequestParam(required = false) String password) {
        
        System.out.println("📥 Запрос на скачивание shortId: " + shortId);
        
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

    @Operation(
        summary = "Предпросмотр файла",
        description = "Показывает файл в браузере (для изображений и PDF) вместо скачивания"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Файл отображён в браузере"),
        @ApiResponse(responseCode = "404", description = "Ссылка не найдена"),
        @ApiResponse(responseCode = "415", description = "Тип файла не поддерживается для предпросмотра")
    })
    @Transactional(readOnly = true)
    @GetMapping("/s/{shortId}/preview")
    public ResponseEntity<InputStreamResource> previewFile(
            @Parameter(description = "Короткий идентификатор ссылки")
            @PathVariable String shortId,
            @Parameter(description = "Пароль для скачивания (если установлен)")
            @RequestParam(required = false) String password) {
        
        ShareLink link = shareLinkRepository.findByShortId(shortId)
                .orElseThrow(() -> new RuntimeException("Ссылка не найдена или истекла"));
        
        FileEntity file = link.getFile();
        
        // Проверяем, что файл можно предпросмотреть (изображения и PDF)
        String mimeType = file.getMimeType();
        if (!mimeType.startsWith("image/") && !mimeType.equals("application/pdf")) {
            throw new RuntimeException("Предпросмотр недоступен для этого типа файла");
        }
        
        InputStream inputStream = shareService.getFileByShortId(shortId, password);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                // inline — показать в браузере, а не скачивать
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + 
                        URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20"))
                .body(new InputStreamResource(inputStream));
    }

    @Operation(
        summary = "Сгенерировать QR-код для ссылки",
        description = "Возвращает PNG-изображение QR-кода, ведущего на страницу скачивания"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "QR-код успешно сгенерирован")
    })
    @GetMapping("/s/{shortId}/qr")
    public void generateQrCode(
            @Parameter(description = "Короткий идентификатор ссылки")
            @PathVariable String shortId,
            jakarta.servlet.http.HttpServletResponse response) throws IOException {
        
        String shareUrl = "http://localhost:8080/s/" + shortId;
        BufferedImage qrImage = QrCodeGenerator.generateQrCode(shareUrl, 300, 300);
        
        response.setContentType("image/png");
        ImageIO.write(qrImage, "png", response.getOutputStream());
    }

    @Operation(
        summary = "Статистика скачиваний",
        description = "Возвращает количество скачиваний и последние записи статистики"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Статистика успешно получена"),
        @ApiResponse(responseCode = "404", description = "Ссылка не найдена")
    })
    @Transactional(readOnly = true)
    @GetMapping("/api/links/{shortId}/stats")
    @ResponseBody
    public ResponseEntity<?> getStats(
            @Parameter(description = "Короткий идентификатор ссылки")
            @PathVariable String shortId) {
        
        ShareLink link = shareLinkRepository.findByShortId(shortId)
                .orElseThrow(() -> new RuntimeException("Ссылка не найдена"));
        
        // Получаем статистику скачиваний
        List<DownloadStat> stats = shareLinkRepository.getDownloadStatsByLinkId(link.getId());
        
        return ResponseEntity.ok(new StatsResponse(
                link.getDownloadsCount(),
                link.getMaxDownloads(),
                link.getExpiresAt(),
                stats
        ));
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " Б";
        else if (bytes < 1024 * 1024) return new DecimalFormat("#.##").format(bytes / 1024.0) + " КБ";
        else if (bytes < 1024 * 1024 * 1024) return new DecimalFormat("#.##").format(bytes / (1024.0 * 1024.0)) + " МБ";
        else return new DecimalFormat("#.##").format(bytes / (1024.0 * 1024.0 * 1024.0)) + " ГБ";
    }

    // DTO для ответа статистики
    public record StatsResponse(
            int downloadsCount,
            Integer maxDownloads,
            java.time.OffsetDateTime expiresAt,
            List<DownloadStat> recentDownloads
    ) {}
}