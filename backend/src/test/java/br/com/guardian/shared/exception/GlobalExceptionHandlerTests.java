package br.com.guardian.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestErrorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve formatar erro de validação com 422 e detalhes por campo")
    void shouldHandleValidationException() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.path").value("/test/errors/validation"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message").value("O nome não pode ser vazio."));
    }

    @Test
    @DisplayName("Deve formatar erro de recurso não encontrado com 404")
    void shouldHandleResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Municipality com identificador '123e4567-e89b-12d3-a456-426614174000' não foi encontrado."));
    }

    @Test
    @DisplayName("Deve formatar erro de regra de negócio com 422")
    void shouldHandleBusinessException() throws Exception {
        mockMvc.perform(get("/test/errors/business"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("MUNICIPALITY_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Município já cadastrado."));
    }

    @Test
    @DisplayName("Deve formatar erro de acesso negado com 403")
    void shouldHandleAccessDeniedException() throws Exception {
        mockMvc.perform(get("/test/errors/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Usuário sem permissão para esta localidade."));
    }

    @Test
    @DisplayName("Deve formatar erro de endpoint não encontrado com 404")
    void shouldHandleNoResourceFoundException() throws Exception {
        mockMvc.perform(get("/test/errors/non-existent-route"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"));
    }
}
