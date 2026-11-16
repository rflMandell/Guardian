package br.com.guardian.integration;

import br.com.guardian.alert.api.dto.CloseAlertRequest;
import br.com.guardian.alert.api.dto.CreateAlertRequest;
import br.com.guardian.alert.domain.model.AlertOriginType;
import br.com.guardian.geolocation.util.GeometryUtils;
import br.com.guardian.identity.api.dto.LoginRequest;
import br.com.guardian.identity.api.dto.RegisterRequest;
import br.com.guardian.municipality.domain.model.Municipality;
import br.com.guardian.municipality.domain.repository.MunicipalityRepository;
import br.com.guardian.occurrence.api.dto.CoordinatesDto;
import br.com.guardian.occurrence.api.dto.CreateOccurrenceRequest;
import br.com.guardian.occurrence.api.dto.UpdateOccurrenceStatusRequest;
import br.com.guardian.occurrence.domain.model.EventCategory;
import br.com.guardian.occurrence.domain.model.ReportStatus;
import br.com.guardian.risk.api.dto.RecalculateRiskRequest;
import br.com.guardian.risk.domain.model.RiskLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Suite de Testes de Integração Completa e Validação End-to-End (Etapa 9 / DEMO_SCRIPT).
 * Valida o funcionamento integrado dos módulos de identidade, municípios, ocorrências,
 * avaliação determinística de riscos, emissão de alertas e pontos seguros com RBAC e isolamento municipal.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndIntegrationFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    private static String citizenToken;
    private static String operatorToken;
    private static String adminToken;
    private static UUID spMunicipalityId;
    private static UUID campinasMunicipalityId;
    private static UUID createdReportId;
    private static UUID createdAlertId;

    @BeforeEach
    void setupData() {
        // Assegura municípios
        Municipality sp = municipalityRepository.findByIbgeCode("3550308").orElseGet(() -> {
            Municipality m = new Municipality("São Paulo", "SP", "3550308", "BR");
            Coordinate[] coords = new Coordinate[]{
                    new Coordinate(-47.0, -24.1), new Coordinate(-46.2, -24.1),
                    new Coordinate(-46.2, -23.3), new Coordinate(-47.0, -23.3),
                    new Coordinate(-47.0, -24.1)
            };
            Polygon poly = GeometryUtils.getFactory().createPolygon(coords);
            m.setBoundary(GeometryUtils.createMultiPolygon(poly));
            return municipalityRepository.save(m);
        });
        spMunicipalityId = sp.getId();

        Municipality campinas = municipalityRepository.findByIbgeCode("3509502").orElseGet(() -> {
            Municipality m = new Municipality("Campinas", "SP", "3509502", "BR");
            Coordinate[] coords = new Coordinate[]{
                    new Coordinate(-47.3, -23.1), new Coordinate(-46.9, -23.1),
                    new Coordinate(-46.9, -22.7), new Coordinate(-47.3, -22.7),
                    new Coordinate(-47.3, -23.1)
            };
            Polygon poly = GeometryUtils.getFactory().createPolygon(coords);
            m.setBoundary(GeometryUtils.createMultiPolygon(poly));
            return municipalityRepository.save(m);
        });
        campinasMunicipalityId = campinas.getId();
    }

    @Test
    @Order(1)
    @DisplayName("E2E-001: Validar cadastro e autenticação de usuários (Cidadão, Operador e Admin)")
    void testAuthenticationFlow() throws Exception {
        // 1. Cadastrar novo cidadão público
        RegisterRequest registerReq = new RegisterRequest("Maria Souza", "maria.souza@example.com", "SenhaForte@2026");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("maria.souza@example.com"));

        // 2. Login com a conta de cidadão cadastrada
        LoginRequest loginCitizen = new LoginRequest("maria.souza@example.com", "SenhaForte@2026");
        MvcResult citizenLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginCitizen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode citizenJson = objectMapper.readTree(citizenLoginResult.getResponse().getContentAsString());
        citizenToken = citizenJson.path("data").path("accessToken").asText();
        assertThat(citizenToken).isNotBlank();

        // 3. Login com o Operador Municipal de Demonstração
        LoginRequest loginOp = new LoginRequest("operador.demo@example.com", "SenhaSegura@123");
        MvcResult opLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginOp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode opJson = objectMapper.readTree(opLoginResult.getResponse().getContentAsString());
        operatorToken = opJson.path("data").path("accessToken").asText();
        assertThat(operatorToken).isNotBlank();

        // 4. Login com o Administrador Global de Demonstração
        LoginRequest loginAdmin = new LoginRequest("admin.demo@example.com", "SenhaSegura@123");
        MvcResult adminLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode adminJson = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString());
        adminToken = adminJson.path("data").path("accessToken").asText();
        assertThat(adminToken).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("E2E-002: Validar envio de ocorrência pelo cidadão e triagem no painel do operador")
    void testOccurrenceReportAndOperatorTriage() throws Exception {
        // Cidadão envia relato de alagamento com GPS na região de São Paulo
        CreateOccurrenceRequest reportReq = new CreateOccurrenceRequest(
                EventCategory.FLOODING,
                "Acúmulo grave de água impedindo passagem de pedestres e veículos.",
                new CoordinatesDto(-23.5505, -46.6333),
                Instant.now(),
                false
        );

        MvcResult reportResult = mockMvc.perform(post("/api/v1/occurrences")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andReturn();

        JsonNode reportJson = objectMapper.readTree(reportResult.getResponse().getContentAsString());
        createdReportId = UUID.fromString(reportJson.path("data").path("id").asText());

        // Operador consulta ocorrências recebidas
        mockMvc.perform(get("/api/v1/occurrences?municipalityId=" + spMunicipalityId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pagination.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // Operador altera status da ocorrência para UNDER_ANALYSIS com parecer técnico
        UpdateOccurrenceStatusRequest updateReq = new UpdateOccurrenceStatusRequest(
                ReportStatus.UNDER_ANALYSIS,
                "Equipe técnica da Defesa Civil enviada para vistoria no local."
        );

        mockMvc.perform(patch("/api/v1/admin/occurrences/" + createdReportId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_ANALYSIS"));
    }

    @Test
    @Order(3)
    @DisplayName("E2E-003: Validar recálculo determinístico e consulta pública da avaliação de risco")
    void testRiskAssessmentRecalculationAndQuery() throws Exception {
        // Operador solicita recálculo de risco para São Paulo
        RecalculateRiskRequest recalcReq = new RecalculateRiskRequest(spMunicipalityId, null, EventCategory.FLOODING);

        mockMvc.perform(post("/api/v1/admin/risk-assessments/recalculate")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recalcReq)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.assessmentId").isNotEmpty());

        // Cidadão consulta avaliação pública mais recente
        mockMvc.perform(get("/api/v1/risk-assessments/latest?municipalityId=" + spMunicipalityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riskLevel").isNotEmpty())
                .andExpect(jsonPath("$.data.factors[0].name").isNotEmpty())
                .andExpect(jsonPath("$.data.confidence").isNotEmpty())
                .andExpect(jsonPath("$.data.dataStatus").value("VALID"));
    }

    @Test
    @Order(4)
    @DisplayName("E2E-004: Validar emissão oficial de alerta pelo operador e consulta pelo cidadão")
    void testAlertIssuanceAndConsultation() throws Exception {
        // Operador emite Alerta Guardian oficial
        Instant expiresAt = Instant.now().plus(4, ChronoUnit.HOURS);
        CreateAlertRequest alertReq = new CreateAlertRequest(
                spMunicipalityId,
                "Alerta de Risco de Alagamento Iminente",
                "Forte chuva acumulada nas últimas horas com risco de transbordamento.",
                RiskLevel.HIGH,
                AlertOriginType.GUARDIAN,
                Instant.now(),
                expiresAt,
                List.of("Evite trafegar por vias alagadas", "Procure abrigo seguro em cotas elevadas"),
                null,
                null,
                "Região Central e Marginal"
        );

        MvcResult alertResult = mockMvc.perform(post("/api/v1/admin/alerts")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alertReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.severity").value("HIGH"))
                .andExpect(jsonPath("$.data.recommendations").isArray())
                .andReturn();

        JsonNode alertJson = objectMapper.readTree(alertResult.getResponse().getContentAsString());
        createdAlertId = UUID.fromString(alertJson.path("data").path("id").asText());

        // Cidadão consulta alertas ativos para o município no aplicativo mobile
        mockMvc.perform(get("/api/v1/alerts?municipalityId=" + spMunicipalityId + "&activeOnly=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Alerta de Risco de Alagamento Iminente"))
                .andExpect(jsonPath("$.data[0].status").value("PUBLISHED"));
    }

    @Test
    @Order(5)
    @DisplayName("E2E-005: Validar pontos seguros e regra crítica de segurança RN042 (lotação/disponibilidade)")
    void testSafeLocationCapacityAndRN042() throws Exception {
        // Consulta apenas pontos com disponibilidade AVAILABLE (availability=AVAILABLE)
        mockMvc.perform(get("/api/v1/safe-locations?municipalityId=" + spMunicipalityId + "&availability=AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Ginásio Poliesportivo do Ibirapuera"))
                .andExpect(jsonPath("$.data[0].availability").value("AVAILABLE"))
                // Garante que o abrigo cheio (Escola Municipal de Acolhimento Sé) NÃO aparece como disponível (RN042)
                .andExpect(jsonPath("$.data[?(@.name == 'Escola Municipal de Acolhimento Sé')]").doesNotExist());

        // Consulta geral sem filtro de disponibilidade retorna todos os locais
        mockMvc.perform(get("/api/v1/safe-locations?municipalityId=" + spMunicipalityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Escola Municipal de Acolhimento Sé')].availability").value("FULL"));
    }

    @Test
    @Order(6)
    @DisplayName("E2E-006: Validar regras de isolamento municipal e restrição RBAC")
    void testMunicipalIsolationAndRbacEnforcement() throws Exception {
        // 1. Operador de São Paulo tenta emitir alerta em Campinas -> DEVE SER RECUSADO COM 403 FORBIDDEN
        CreateAlertRequest unauthorizedAlert = new CreateAlertRequest(
                campinasMunicipalityId,
                "Tentativa de emissão fora do escopo",
                "Descrição...",
                RiskLevel.HIGH,
                AlertOriginType.GUARDIAN,
                Instant.now(),
                Instant.now().plus(2, ChronoUnit.HOURS),
                List.of("Recomendação"),
                null,
                null,
                "Região"
        );

        mockMvc.perform(post("/api/v1/admin/alerts")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unauthorizedAlert)))
                .andExpect(status().isForbidden());

        // 2. Cidadão comum tenta emitir alerta administrativo -> DEVE SER RECUSADO COM 403 FORBIDDEN
        mockMvc.perform(post("/api/v1/admin/alerts")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unauthorizedAlert)))
                .andExpect(status().isForbidden());

        // 3. Usuário anônimo tenta acessar rota administrativa -> DEVE SER RECUSADO COM 401 UNAUTHORIZED
        mockMvc.perform(post("/api/v1/admin/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unauthorizedAlert)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("E2E-007: Validar encerramento de alerta com justificativa obrigatória (RN033)")
    void testAlertClosingWithMandatoryReason() throws Exception {
        // 1. Tentar encerrar alerta sem justificativa -> DEVE FALHAR (422)
        mockMvc.perform(post("/api/v1/admin/alerts/" + createdAlertId + "/close")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CloseAlertRequest("   "))))
                .andExpect(status().isUnprocessableEntity());

        // 2. Encerrar fornecendo justificativa válida da Defesa Civil
        mockMvc.perform(post("/api/v1/admin/alerts/" + createdAlertId + "/close")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CloseAlertRequest(
                                "Nível pluviométrico estabilizado e vias desobstruídas pela equipe de campo."
                        ))))
                .andExpect(status().isNoContent());

        // 3. Confirmar que o alerta não está mais ativo para o cidadão
        mockMvc.perform(get("/api/v1/alerts?municipalityId=" + spMunicipalityId + "&activeOnly=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + createdAlertId + "')]").doesNotExist());
    }
}
