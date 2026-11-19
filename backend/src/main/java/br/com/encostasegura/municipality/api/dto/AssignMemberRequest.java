package br.com.encostasegura.municipality.api.dto;

import br.com.encostasegura.accesscontrol.domain.model.MembershipStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignMemberRequest(
        @NotNull(message = "O identificador do usuário é obrigatório.")
        UUID userId,

        MembershipStatus status
) {}
