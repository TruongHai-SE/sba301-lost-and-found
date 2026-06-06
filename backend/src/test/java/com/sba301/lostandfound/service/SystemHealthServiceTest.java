package com.sba301.lostandfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sba301.lostandfound.client.DatabaseHealthClient;
import com.sba301.lostandfound.client.ClipClient;
import com.sba301.lostandfound.service.impl.SystemHealthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemHealthServiceTest {

    @Mock
    private DatabaseHealthClient databaseHealthClient;

    @Mock
    private ClipClient clipClient;

    @InjectMocks
    private SystemHealthServiceImpl systemHealthService;

    @Test
    void reportsHealthyDependencies() {
        when(clipClient.getHealthStatus()).thenReturn("ok");

        var response = systemHealthService.getHealth();

        assertThat(response.backend()).isEqualTo("ok");
        assertThat(response.database()).isEqualTo("ok");
        assertThat(response.clip()).isEqualTo("ok");
    }

    @Test
    void reportsUnavailableClipWithoutFailingBackendHealth() {
        when(clipClient.getHealthStatus()).thenThrow(
            new IllegalStateException("offline")
        );

        var response = systemHealthService.getHealth();

        assertThat(response.backend()).isEqualTo("ok");
        assertThat(response.database()).isEqualTo("ok");
        assertThat(response.clip()).isEqualTo("unavailable");
    }
}
