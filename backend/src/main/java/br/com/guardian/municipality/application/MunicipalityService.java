package br.com.guardian.municipality.application;

import br.com.guardian.accesscontrol.application.MunicipalScopeService;
import br.com.guardian.accesscontrol.domain.model.MembershipStatus;
import br.com.guardian.accesscontrol.domain.model.MunicipalityMembership;
import br.com.guardian.accesscontrol.domain.repository.MunicipalityMembershipRepository;
import br.com.guardian.municipality.api.dto.*;
import br.com.guardian.municipality.domain.model.Municipality;
import br.com.guardian.municipality.domain.repository.MunicipalityRepository;
import br.com.guardian.shared.exception.BusinessException;
import br.com.guardian.shared.exception.ResourceNotFoundException;
import br.com.guardian.usermanagement.domain.model.User;
import br.com.guardian.usermanagement.domain.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MunicipalityService {

    private final MunicipalityRepository municipalityRepository;
    private final UserRepository userRepository;
    private final MunicipalityMembershipRepository membershipRepository;
    private final MunicipalScopeService municipalScopeService;

    public MunicipalityService(MunicipalityRepository municipalityRepository,
                               UserRepository userRepository,
                               MunicipalityMembershipRepository membershipRepository,
                               MunicipalScopeService municipalScopeService) {
        this.municipalityRepository = municipalityRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.municipalScopeService = municipalScopeService;
    }

    @Transactional
    public MunicipalityResponseData createMunicipality(CreateMunicipalityRequest request) {
        String cleanIbge = request.ibgeCode().trim();
        if (municipalityRepository.existsByIbgeCode(cleanIbge)) {
            throw new BusinessException("IBGE_CODE_ALREADY_EXISTS",
                    String.format("Já existe um município cadastrado com o código IBGE '%s'.", cleanIbge));
        }

        Municipality municipality = new Municipality(
                request.name().trim(),
                request.state().trim().toUpperCase(),
                cleanIbge,
                request.country() != null ? request.country().trim().toUpperCase() : "BR"
        );

        Municipality saved = municipalityRepository.save(municipality);
        return toResponseData(saved);
    }

    @Transactional(readOnly = true)
    public Page<MunicipalityResponseData> listMunicipalities(String search, String state, Pageable pageable) {
        Page<Municipality> page;

        boolean hasSearch = StringUtils.hasText(search);
        boolean hasState = StringUtils.hasText(state);

        if (hasSearch && hasState) {
            page = municipalityRepository.findByNameContainingIgnoreCaseAndStateIgnoreCase(search.trim(), state.trim(), pageable);
        } else if (hasSearch) {
            page = municipalityRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        } else if (hasState) {
            page = municipalityRepository.findByStateIgnoreCase(state.trim(), pageable);
        } else {
            page = municipalityRepository.findAll(pageable);
        }

        return page.map(this::toResponseData);
    }

    @Transactional(readOnly = true)
    public MunicipalityResponseData getMunicipality(UUID id) {
        Municipality municipality = findById(id);
        return toResponseData(municipality);
    }

    @Transactional(readOnly = true)
    public MunicipalityStatusResponseData getMunicipalityStatus(UUID id) {
        Municipality municipality = findById(id);

        // Por enquanto, retorno do status com baseline determinístico
        return new MunicipalityStatusResponseData(
                municipality.getId(),
                "LOW",
                0,
                "CURRENT",
                Instant.now()
        );
    }

    @Transactional
    public MunicipalityMemberData assignMember(UUID municipalityId, AssignMemberRequest request) {
        Municipality municipality = findById(municipalityId);
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", request.userId()));

        MembershipStatus status = request.status() != null ? request.status() : MembershipStatus.ACTIVE;

        MunicipalityMembership membership = membershipRepository
                .findByUserIdAndMunicipalityId(user.getId(), municipality.getId())
                .orElseGet(() -> new MunicipalityMembership(user, municipality.getId(), status));

        membership.setStatus(status);
        MunicipalityMembership saved = membershipRepository.save(membership);

        return toMemberData(saved, user);
    }

    @Transactional(readOnly = true)
    public List<MunicipalityMemberData> listMembers(UUID municipalityId, UUID requestingUserId) {
        // Valida escopo: operador A tentando acessar município B é bloqueado (RN001 / RBAC-002)
        municipalScopeService.validateScope(requestingUserId, municipalityId);

        findById(municipalityId); // Garante que município existe

        return membershipRepository.findAll().stream()
                .filter(m -> m.getMunicipalityId().equals(municipalityId))
                .map(m -> toMemberData(m, m.getUser()))
                .toList();
    }

    private Municipality findById(UUID id) {
        return municipalityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Município", id));
    }

    private MunicipalityResponseData toResponseData(Municipality m) {
        return new MunicipalityResponseData(
                m.getId(),
                m.getName(),
                m.getState(),
                m.getCountryCode(),
                m.getIbgeCode(),
                m.getStatus().name()
        );
    }

    private MunicipalityMemberData toMemberData(MunicipalityMembership m, User u) {
        return new MunicipalityMemberData(
                m.getId(),
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                m.getMunicipalityId(),
                m.getStatus().name(),
                m.getCreatedAt()
        );
    }
}
