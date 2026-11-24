package vn.edu.tmdt.identity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.tmdt.identity.domain.Role;
import vn.edu.tmdt.identity.domain.User;
import vn.edu.tmdt.identity.domain.UserRepository;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    public ApplicationRunner seedUsers(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            create(users, encoder, "admin@demo.vn", "Admin@123", "Quản trị viên", Role.ADMIN);
            create(users, encoder, "organizer@demo.vn", "Org@12345", "Nhà tổ chức Demo", Role.ORGANIZER);
            create(users, encoder, "customer@demo.vn", "User@12345", "Khách Hàng Demo", Role.CUSTOMER);
            log.info("Da seed 3 tai khoan demo");
        };
    }

    private void create(UserRepository users, PasswordEncoder encoder, String email, String password,
                        String fullName, Role role) {
        User user = new User();
        user.email = email;
        user.password = encoder.encode(password);
        user.fullName = fullName;
        user.role = role;
        user.enabled = true;
        users.save(user);
    }
}
