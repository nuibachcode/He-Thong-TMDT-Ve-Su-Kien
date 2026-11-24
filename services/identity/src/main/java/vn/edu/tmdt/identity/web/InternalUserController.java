package vn.edu.tmdt.identity.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.identity.domain.UserRepository;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserRepository users;

    public InternalUserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/count")
    public Map<String, Long> count() {
        return Map.of("users", users.count());
    }
}
