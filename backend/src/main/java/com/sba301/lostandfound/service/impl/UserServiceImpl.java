package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.dto.AdminUpdateUserRequest;
import com.sba301.lostandfound.dto.ChangeUserRoleRequest;
import com.sba301.lostandfound.dto.PageResponse;
import com.sba301.lostandfound.dto.UserResponse;
import com.sba301.lostandfound.entity.User;
import com.sba301.lostandfound.entity.enums.UserType;
import com.sba301.lostandfound.repository.MatchRequestRepository;
import com.sba301.lostandfound.repository.RefreshTokenRepository;
import com.sba301.lostandfound.repository.UserRepository;
import com.sba301.lostandfound.service.UserService;
import com.sba301.lostandfound.util.StringSanitizer;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    /** Maximum page size that can be requested by a client. */
    private static final int MAX_PAGE_SIZE = 50;

    /** Allowed sort fields — prevents arbitrary field injection into ORDER BY. */
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "name", "mail", "createdAt");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MatchRequestRepository matchRequestRepository;

    public UserServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           MatchRequestRepository matchRequestRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.matchRequestRepository = matchRequestRepository;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(int page, int size, String search,
                                                UserType role, String sortBy, String sortDir) {
        // Validate inputs — no silent clamping; bad input → 400
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page index must be >= 0");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid sortBy field '" + sortBy + "'. Allowed: " + ALLOWED_SORT_FIELDS);
        }
        if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sortDir must be 'asc' or 'desc'");
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // Normalize search: null, empty, or blank → treat as no filter
        String clean = (search == null || search.isBlank()) ? null : StringSanitizer.sanitizeSearchText(search);
        String searchTerm = (clean == null || clean.isBlank()) ? null : clean;
        Page<User> userPage = userRepository.findBySearchAndRole(searchTerm, role, pageable);

        return PageResponse.from(userPage.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findUserOrThrow(id);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, AdminUpdateUserRequest request) {
        User user = findUserOrThrow(id);

        // Phone uniqueness check — only when phone is changing to a non-null value
        String newPhone = request.getPhone();
        if (newPhone != null && !newPhone.isBlank()
                && userRepository.existsByPhoneAndIdNot(newPhone, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phone number is already in use by another account");
        }

        if (request.getName() != null) {
            user.setName(request.getName().isBlank() ? null : request.getName().strip());
        }
        // null phone → clear; blank string → clear; valid digits → set
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().isBlank() ? null : request.getPhone());
        }

        userRepository.save(user);
        log.info("Admin updated user id={}", id);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse changeUserRole(Long id, ChangeUserRoleRequest request, User currentAdmin) {
        // Guard: admin cannot change their own role
        if (currentAdmin.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admins cannot change their own role");
        }

        User user = findUserOrThrow(id);

        UserType previousRole = user.getType();
        UserType newRole = request.getNewRole();

        if (previousRole == newRole) {
            // No-op — still return current state without revoking tokens
            return toResponse(user);
        }

        // Guard: cannot demote the last ADMIN to USER (would leave system with no admins)
        if (previousRole == UserType.ADMIN && newRole == UserType.USER) {
            long adminCount = userRepository.countByType(UserType.ADMIN);
            if (adminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Cannot demote the last admin account");
            }
        }

        user.setType(newRole);
        userRepository.save(user);

        // Revoke all refresh tokens so the role claim in existing tokens is invalidated.
        // Note: Access tokens (stateless JWT) remain valid until expiry (~30 min by default).
        // This is a known limitation; no access-token blacklist is implemented.
        refreshTokenRepository.revokeAllByUserId(id);
        log.info("Admin changed role of user id={} from {} to {}; refresh tokens revoked",
                id, previousRole, newRole);

        return toResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, User currentAdmin) {
        User target = findUserOrThrow(id);

        // Guard: admin cannot delete their own account
        if (currentAdmin.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admins cannot delete their own account");
        }

        // Guard: cannot delete the last ADMIN
        if (target.getType() == UserType.ADMIN) {
            long adminCount = userRepository.countByType(UserType.ADMIN);
            if (adminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Cannot delete the last admin account");
            }
        }

        // Guard: user still has posts — posts.user_id has no ON DELETE CASCADE.
        // Deleting user while posts exist would cause a FK violation at the DB level.
        if (userRepository.hasAnyPost(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete user: they still have posts. Remove or reassign posts first.");
        }

        // Guard: user still has match requests — match_requests.user_id has no ON DELETE CASCADE.
        if (matchRequestRepository.existsByUserId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete user: they still have match requests.");
        }

        // refresh_tokens and otp_tokens use ON DELETE CASCADE — DB removes them automatically.
        userRepository.delete(target);
        log.info("Admin deleted user id={} (type={})", id, target.getType());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .mail(user.getMail())
                .phone(user.getPhone())
                .socialLink(user.getSocialLink())
                .type(user.getType())
                .createdAt(user.getCreatedAt())
                .googleAccount(user.getPassword() == null)
                .hasPassword(user.getPassword() != null)
                .build();
    }
}
