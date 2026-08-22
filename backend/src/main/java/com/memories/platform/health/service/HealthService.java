package com.memories.platform.health.service;

import com.memories.platform.health.dto.HealthResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final JdbcClient jdbcClient;

    public HealthService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public HealthResponse getHealth() {
        Integer databaseProbe = jdbcClient.sql("select 1")
                .query(Integer.class)
                .single();

        if (databaseProbe == 1) {
            return new HealthResponse("UP", "UP");
        }

        return new HealthResponse("DEGRADED", "DOWN");
    }
}
