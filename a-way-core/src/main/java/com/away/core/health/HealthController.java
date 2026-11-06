package com.away.core.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "a-way-core");
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            body.put("database", valid ? "UP" : "DOWN");
            body.put("status", valid ? "UP" : "DOWN");
        } catch (SQLException exception) {
            body.put("database", "DOWN");
            body.put("status", "DOWN");
        }
        return body;
    }
}
