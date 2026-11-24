package vn.edu.tmdt.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tmdt.auth.dto.AuthResponse;
import vn.edu.tmdt.auth.dto.LoginRequest;
import vn.edu.tmdt.auth.dto.RegisterRequest;
import vn.edu.tmdt.auth.dto.UserResponse;
import vn.edu.tmdt.common.exception.AppException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw AppException.conflict("Email nay da duoc dang ky");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);

        log.info("Dang ky thanh cong tai khoan {}", email);
        return issueToken(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> AppException.unauthorized("Tai khoan khong ton tai"));

        return issueToken(user);
    }

    public UserResponse me(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> AppException.unauthorized("Phien dang nhap khong hop le"));
        return toResponse(user);
    }

    private AuthResponse issueToken(User user) {
        String token = jwtService.generate(user);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), toResponse(user));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                user.getRole().name());
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
