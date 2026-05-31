package com.sba301.lostandfound.infrastructure.persistence;

import com.sba301.lostandfound.application.port.out.CheckDatabaseHealthPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthAdapter implements CheckDatabaseHealthPort {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void checkHealth() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    }
}
