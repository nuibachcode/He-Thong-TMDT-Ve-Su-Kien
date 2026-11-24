package vn.edu.tmdt.identity.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.identity.common.ApiResponse;
import vn.edu.tmdt.identity.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public ApiResponse<AuthService.AuthView> register(@Valid @RequestBody AuthService.RegisterRequest request) {
        return ApiResponse.ok("Dang ky thanh cong", auth.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.AuthView> login(@Valid @RequestBody AuthService.LoginRequest request) {
        return ApiResponse.ok("Dang nhap thanh cong", auth.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthService.UserView> me(
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        return ApiResponse.ok(auth.me(email));
    }
}
