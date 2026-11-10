package br.com.guardian.usermanagement.application;

import br.com.guardian.accesscontrol.domain.model.Role;
import br.com.guardian.shared.exception.ResourceNotFoundException;
import br.com.guardian.usermanagement.api.dto.UpdateProfileRequest;
import br.com.guardian.usermanagement.api.dto.UserProfileData;
import br.com.guardian.usermanagement.domain.model.User;
import br.com.guardian.usermanagement.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileData getMyProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));

        return toProfileData(user);
    }

    @Transactional
    public UserProfileData updateMyProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));

        if (StringUtils.hasText(request.name())) {
            user.setFullName(request.name().trim());
        }

        if (StringUtils.hasText(request.preferredLanguage())) {
            user.setPreferredLanguage(request.preferredLanguage().trim());
        }

        User updated = userRepository.save(user);
        return toProfileData(updated);
    }

    private UserProfileData toProfileData(User user) {
        return new UserProfileData(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAccountType().name(),
                user.getPreferredLanguage(),
                user.isEmailVerified(),
                user.getRoles().stream().map(Role::getName).toList()
        );
    }
}
