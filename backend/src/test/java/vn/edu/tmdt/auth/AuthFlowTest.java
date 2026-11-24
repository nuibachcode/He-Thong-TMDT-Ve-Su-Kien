package vn.edu.tmdt.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tmdt.auth.dto.AuthResponse;
import vn.edu.tmdt.auth.dto.LoginRequest;
import vn.edu.tmdt.auth.dto.RegisterRequest;
import vn.edu.tmdt.common.exception.AppException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthFlowTest {

    @Autowired
    private AuthService authService;

    @Test
    void registerThenLoginThenMe() {
        AuthResponse registered = authService.register(
                new RegisterRequest("newbie@test.vn", "Secret@123", "Nguoi Dung Moi", "0901234567"));

        assertThat(registered.accessToken()).isNotBlank();
        assertThat(registered.user().role()).isEqualTo("CUSTOMER");

        AuthResponse loggedIn = authService.login(new LoginRequest("newbie@test.vn", "Secret@123"));
        assertThat(loggedIn.accessToken()).isNotBlank();

        assertThat(authService.me("newbie@test.vn").email()).isEqualTo("newbie@test.vn");
    }

    @Test
    void duplicateEmailIsRejected() {
        authService.register(new RegisterRequest("dup@test.vn", "Secret@123", "Lan Mot", null));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("dup@test.vn", "Secret@123", "Lan Hai", null)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("da duoc dang ky");
    }

    @Test
    void wrongPasswordIsRejected() {
        authService.register(new RegisterRequest("wrong@test.vn", "Secret@123", "Sai Mat Khau", null));

        assertThatThrownBy(() -> authService.login(new LoginRequest("wrong@test.vn", "KhongDung@123")))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }
}
