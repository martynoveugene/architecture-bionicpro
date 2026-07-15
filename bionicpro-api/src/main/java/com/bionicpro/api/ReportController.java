package com.bionicpro.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final Logger log = LoggerFactory.getLogger(ClassUtils.getUserClass(getClass()));

    private final JdbcTemplate jdbcTemplate;

    public ReportController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
    public ResponseEntity<byte[]> getReportByDay(@PathVariable String day, @AuthenticationPrincipal Jwt jwt) {
        String customerId = jwt.getClaimAsString("preferred_username");

        String sql = "SELECT toString(day) as day, customerId, avg_temperature, min_chargeLevel, max_chargeLevel " +
                "FROM bio_sensor_daily WHERE customerId = ? AND day = ? LIMIT 1";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, customerId, day);

        if (results.isEmpty()) {
            throw new RuntimeException("Отчет за день " + day + " не найден для вашего аккаунта.");
        }

        Map<String, Object> report = results.get(0);

        StringBuilder textContent = new StringBuilder();
        textContent.append("=== ОТЧЕТ ПО СЕНСОРУ ===\n");
        textContent.append("Пользователь: ").append(report.get("customerId")).append("\n");
        textContent.append("Дата: ").append(report.get("day")).append("\n");
        textContent.append("---------------------------\n");
        textContent.append("Средняя температура: ").append(report.get("avg_temperature")).append(" °C\n");
        textContent.append("Минимальный заряд: ").append(report.get("min_chargeLevel")).append("%\n");
        textContent.append("Максимальный заряд: ").append(report.get("max_chargeLevel")).append("%\n");
        textContent.append("===========================\n");

        byte[] fileBytes = textContent.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "report-" + day + ".txt");

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileBytes);
    }
}
