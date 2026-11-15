package com.iitsm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class IitsmApplicationTests {

    @Test
    void contextLoads() {
        // Verifies all Spring components, repositories, and configurations wire properly
    }
}
