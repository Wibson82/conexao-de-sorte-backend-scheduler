package br.tec.facilitaservicos.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SimpleTest {
    
    @Test
    void contextLoads() {
        assertTrue(true);
    }
}