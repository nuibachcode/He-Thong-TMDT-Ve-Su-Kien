package vn.edu.tmdt.identity.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.identity.common.ApiResponse;
import vn.edu.tmdt.identity.common.AppException;
import vn.edu.tmdt.identity.domain.User;
import vn.edu.tmdt.identity.domain.UserRepository;
import vn.edu.tmdt.identity.service.AuthService;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final UserRepository users;
    private final AuthService auth;

    public AdminUserController(UserRepository users, AuthService auth) {
        this.users = users;
        this.auth = auth;
    }

    @GetMapping("/users")
    public ApiResponse<List<AuthService.UserView>> list(@RequestParam(required = false) String q) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return ApiResponse.ok(users.findAll().stream()
                .filter(user -> needle.isBlank()
                        || user.email.toLowerCase().contains(needle)
                        || user.fullName.toLowerCase().contains(needle))
                .map(auth::view)
                .toList());
    }

    public record StatusRequest(boolean enabled) {
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<String> setStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        User user = users.findById(id).orElseThrow(() -> AppException.notFound("Khong tim thay nguoi dung"));
        user.setEnabled(request.enabled());
        users.save(user);
        return ApiResponse.ok(request.enabled() ? "Da mo khoa tai khoan" : "Da khoa tai khoan", "OK");
    }
}
