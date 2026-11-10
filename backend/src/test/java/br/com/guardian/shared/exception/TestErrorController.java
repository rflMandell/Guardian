package br.com.guardian.shared.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/errors")
public class TestErrorController {

    public record DummyRequest(
            @NotBlank(message = "O nome não pode ser vazio.")
            String name
    ) {}

    @PostMapping("/validation")
    public void testValidation(@Valid @RequestBody DummyRequest req) {}

    @GetMapping("/not-found")
    public void testNotFound() {
        throw new ResourceNotFoundException("Municipality", "123e4567-e89b-12d3-a456-426614174000");
    }

    @GetMapping("/business")
    public void testBusiness() {
        throw new BusinessException("MUNICIPALITY_ALREADY_EXISTS", "Município já cadastrado.");
    }

    @GetMapping("/access-denied")
    public void testAccessDenied() {
        throw new AccessDeniedException("Usuário sem permissão para esta localidade.");
    }
}
