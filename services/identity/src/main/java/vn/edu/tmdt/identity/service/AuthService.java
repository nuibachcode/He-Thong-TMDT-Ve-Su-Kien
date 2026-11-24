package vn.edu.tmdt.identity.service;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tmdt.identity.common.AppException;
import vn.edu.tmdt.identity.domain.Role;
import vn.edu.tmdt.identity.domain.User;
import vn.edu.tmdt.identity.domain.UserRepository;

@Service
public class AuthService {

    public record RegisterRequest(@Email @NotBlank String email,
                                  @NotBlank @Size(min = 8, max = 72) String password,
                                  @NotBlank String fullName,
                                  String phone) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record UserView(Long id, String email, String fullName, String phone, String role,
                           boolean enabled, String createdAt) {
    }

    public record AuthView(String accessToken, String tokenType, long expiresIn, UserView user) {
    }

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthView register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw AppException.conflict("Email da duoc dang ky");
        }
        User user = new User();
        user.email = email;
        user.password = encoder.encode(request.password());
        user.fullName = request.fullName().trim();
        user.phone = request.phone();
        user.role = Role.CUSTOMER;
        user.enabled = true;
        users.save(user);
        return auth(user);
    }

    @Transactional(readOnly = true)
    public AuthView login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> AppException.unauthorized("Email hoac mat khau khong dung"));
        if (!user.enabled) {
            throw AppException.forbidden("Tai khoan da bi khoa");
        }
        if (!encoder.matches(request.password(), user.password)) {
            throw AppException.unauthorized("Email hoac mat khau khong dung");
        }
        return auth(user);
    }

    @Transactional(readOnly = true)
    public UserView me(String email) {
        if (email == null || email.isBlank()) {
            throw AppException.unauthorized("Ban can dang nhap");
        }
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> AppException.unauthorized("Khong tim thay nguoi dung"));
        return view(user);
    }

    private AuthView auth(User user) {
        return new AuthView(jwt.generate(user), "Bearer", jwt.getExpirationSeconds(), view(user));
    }

    public UserView view(User user) {
        return new UserView(user.id, user.email, user.fullName, user.phone, user.role.name(),
                user.enabled, user.createdAt == null ? null : user.createdAt.toString());
    }
}
