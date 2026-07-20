package com.bionicpro.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import java.net.URI;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final Logger log = LoggerFactory.getLogger(ClassUtils.getUserClass(getClass()));

    @Value("${CDN_BASE_URL}")
    private String cdnBaseUrl;

    private final JdbcTemplate jdbcTemplate;
    private final S3Client s3Client;

    public ReportController(JdbcTemplate jdbcTemplate, S3Client s3Client) {
        this.jdbcTemplate = jdbcTemplate;
        this.s3Client = s3Client;
    }

    /**
     * Возвращает список дат отчетов для текущего пользователя из JWT
     */
    @GetMapping
    public List<String> getReportsList(@AuthenticationPrincipal Jwt jwt) {
        // customerId это claim "preferred_username" токена
        String customerId = jwt.getClaimAsString("preferred_username");
        log.info("customerId = "+customerId);

        String sql = "SELECT DISTINCT toString(day) FROM bio_sensor_daily WHERE customerId = ? ORDER BY day DESC";

        return jdbcTemplate.queryForList(sql, String.class, customerId);
    }

    /**
     * Конкретный отчёт за выбранный день (формат YYYY-MM-DD)
     */
    @GetMapping("/{day}")
    public ResponseEntity<Void> getReportByDay(@PathVariable String day, @AuthenticationPrincipal Jwt jwt) {
        String customerId = jwt.getClaimAsString("preferred_username");

        // структура папок: customerId/год/месяц/report-дата.txt
        String year = day.substring(0, 4);
        String month = day.substring(5, 7);
        String bucketName = "sensor-reports";
        String s3Key = String.format("%s/%s/%s/report-%s.txt", customerId, year, month, day);

        // Ссылка на CDN, через которую пользователь скачает файл
        String cdnUrl = String.format("%s/%s/%s", cdnBaseUrl, bucketName, s3Key);

        boolean fileExists = true;
        try {
            // Проверяем метаданные объекта в S3 без скачивания самого файла
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                fileExists = false;
            } else {
                throw e;
            }
        }

        if (!fileExists) {
            String sql = "SELECT toString(day) as day, customerId, avg_temperature, min_chargeLevel, max_chargeLevel " +
                    "FROM bio_sensor_daily WHERE customerId = ? AND day = ? LIMIT 1";

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, customerId, day);

            if (results.isEmpty()) {
                throw new RuntimeException("Отчет за день " + day + " не найден.");
            }

            Map<String, Object> report = results.get(0);

            StringBuilder textContent = new StringBuilder();
            textContent.append("=== ОТЧЕТ ПО СЕНСОРАМ ===\n");
            textContent.append("Пользователь: ").append(report.get("customerId")).append("\n");
            textContent.append("Дата: ").append(report.get("day")).append("\n");
            textContent.append("---------------------------\n");
            textContent.append("Средняя температура: ").append(report.get("avg_temperature")).append(" °C\n");
            textContent.append("Минимальный заряд: ").append(report.get("min_chargeLevel")).append("%\n");
            textContent.append("Максимальный заряд: ").append(report.get("max_chargeLevel")).append("%\n");
            textContent.append("===========================\n");

            byte[] fileBytes = textContent.toString().getBytes(StandardCharsets.UTF_8);

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .contentType("text/plain")
                            .build(),
                    RequestBody.fromBytes(fileBytes)
            );
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(cdnUrl))
                .build();
    }
}
