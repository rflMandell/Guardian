package br.com.guardian;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GuardianApplicationTests {

    @Test
    void contextLoads() {
        // Verifica se o contexto do Spring Boot inicializa corretamente
    }
}
