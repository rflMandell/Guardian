package br.com.encostasegura;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EncostaSeguraApplicationTests {

    @Test
    void contextLoads() {
        // Verifica se o contexto do Spring Boot inicializa corretamente
    }
}
