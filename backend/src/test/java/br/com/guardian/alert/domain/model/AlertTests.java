package br.com.guardian.alert.domain.model;

import br.com.guardian.risk.domain.model.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Alert - Testes unitários do modelo e ciclo de vida de alertas")
class AlertTests {

    @Test
    @DisplayName("RN026 / RN027: Deve instanciar alerta com status PUBLISHED e registrar histórico inicial")
    void shouldCreateAlertWithPublishedStatusAndHistory() {
        UUID munId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expires = now.plus(4, ChronoUnit.HOURS);

        Alert alert = new Alert(
                munId,
                "Alerta de Inundação Gradual",
                "Elevação crítica do nível do rio.",
                RiskLevel.HIGH,
                AlertOriginType.GUARDIAN,
                operatorId,
                now,
                expires
        );

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.PUBLISHED);
        assertThat(alert.isActive()).isTrue();
        assertThat(alert.isExpired()).isFalse();
        assertThat(alert.getStatusHistory()).hasSize(1);
        assertThat(alert.getStatusHistory().get(0).getToStatus()).isEqualTo(AlertStatus.PUBLISHED);
    }

    @Test
    @DisplayName("RN030: Deve encerrar alerta registrando novo status e justificativa no histórico")
    void shouldCloseAlertWithReason() {
        UUID munId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        Alert alert = new Alert(
                munId, "Alerta de Tempestade", "Ventos fortes.",
                RiskLevel.MODERATE, AlertOriginType.GUARDIAN, operatorId,
                Instant.now(), Instant.now().plus(2, ChronoUnit.HOURS)
        );

        alert.close(operatorId, "Nível das águas baixou e o tempo estabilizou.");

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.CLOSED);
        assertThat(alert.isActive()).isFalse();
        assertThat(alert.getStatusHistory()).hasSize(2);
        assertThat(alert.getStatusHistory().get(1).getToStatus()).isEqualTo(AlertStatus.CLOSED);
        assertThat(alert.getStatusHistory().get(1).getReason()).contains("Nível das águas baixou");
    }

    @Test
    @DisplayName("RN029: Deve atualizar alerta para UPDATED e registrar histórico")
    void shouldUpdateAlertDetails() {
        UUID munId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        Alert alert = new Alert(
                munId, "Alerta de Chuva", "Chuva moderada.",
                RiskLevel.MODERATE, AlertOriginType.GUARDIAN, operatorId,
                Instant.now(), Instant.now().plus(2, ChronoUnit.HOURS)
        );

        alert.updateDetails(
                RiskLevel.CRITICAL, "Chuva torrencial com risco extremo de transbordamento.",
                Instant.now().plus(4, ChronoUnit.HOURS), operatorId, "Agravamento severo das condições meteorológicas."
        );

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.UPDATED);
        assertThat(alert.getSeverity()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(alert.getStatusHistory()).hasSize(2);
        assertThat(alert.getStatusHistory().get(1).getToStatus()).isEqualTo(AlertStatus.UPDATED);
    }

    @Test
    @DisplayName("RN030: Deve identificar alerta como EXPIRED quando a validade estiver no passado")
    void shouldDetectExpiredAlert() {
        UUID munId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        Instant pastIssued = Instant.now().minus(6, ChronoUnit.HOURS);
        Instant pastExpires = Instant.now().minus(2, ChronoUnit.HOURS);

        Alert alert = new Alert(
                munId, "Alerta Expirado", "Mensagem.",
                RiskLevel.LOW, AlertOriginType.GUARDIAN, operatorId,
                pastIssued, pastExpires
        );

        assertThat(alert.isExpired()).isTrue();
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.EXPIRED);
        assertThat(alert.isActive()).isFalse();
    }
}
