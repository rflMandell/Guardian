package br.com.guardian.municipality.api.dto;

import br.com.guardian.accesscontrol.domain.model.MembershipStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignMemberRequest(
        @NotNull(message = "O identificador do usuário é obrigatório.")
        UUID userId,

        MembershipStatus status
) {}
