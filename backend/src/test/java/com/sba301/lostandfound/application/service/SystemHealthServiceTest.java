package com.sba301.lostandfound.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sba301.lostandfound.application.port.out.CheckDatabaseHealthPort;
import com.sba301.lostandfound.application.port.out.GetClipHealthPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemHealthServiceTest {

    @Mock
    private CheckDatabaseHealthPort databaseHealthPort;

    @Mock
    private GetClipHealthPort clipHealthPort;

    @InjectMocks
    private SystemHealthService systemHealthService;

    @Test
    void reportsHealthyDependencies() {
        when(clipHealthPort.getHealthStatus()).thenReturn("ok");

        var response = systemHealthService.getHealth();

        assertThat(response.backend()).isEqualTo("ok");
        assertThat(response.database()).isEqualTo("ok");
        assertThat(response.clip()).isEqualTo("ok");
    }

    @Test
    void reportsUnavailableClipWithoutFailingBackendHealth() {
        when(clipHealthPort.getHealthStatus()).thenThrow(
            new IllegalStateException("offline")
        );

        var response = systemHealthService.getHealth();

        assertThat(response.backend()).isEqualTo("ok");
        assertThat(response.database()).isEqualTo("ok");
        assertThat(response.clip()).isEqualTo("unavailable");
    }
}
