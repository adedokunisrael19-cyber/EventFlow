package com.eventflow.common.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    @Test
    void returnsAnUpStatus() {
        HealthController controller = new HealthController();

        assertThat(controller.getHealth()).isEqualTo(new HealthResponse("UP"));
    }
}
