package com.sba301.lostandfound.client;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthClient {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthClient(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void checkHealth() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    }
}
