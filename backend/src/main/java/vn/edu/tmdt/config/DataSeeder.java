package vn.edu.tmdt.config;

import java.util.Map;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.tmdt.auth.Role;
import vn.edu.tmdt.auth.User;
import vn.edu.tmdt.auth.UserRepository;

/**
 * Tao san 3 tai khoan mau de ca nhom dev/test duoc ngay tu ngay dau.
 * Khong tao du lieu nghiep vu (event/tour) - phan do se do nhom tu seed o tuan 2.
 */
@Configuration
@Profile("!test")
public class DataSeeder {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    @org.springframework.core.annotation.Order(1)
    public ApplicationRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            seed(userRepository, passwordEncoder, "admin@demo.vn", "Admin@123", "Quan tri vien", Role.ADMIN);
            seed(userRepository, passwordEncoder, "organizer@demo.vn", "Org@12345", "Nha to chuc Demo", Role.ORGANIZER);
            seed(userRepository, passwordEncoder, "customer@demo.vn", "User@12345", "Khach hang Demo", Role.CUSTOMER);

            log.info("=== TAI KHOAN MAU ===");
            log.info("admin@demo.vn / Admin@123        (ADMIN)");
            log.info("organizer@demo.vn / Org@12345    (ORGANIZER)");
            log.info("customer@demo.vn / User@12345    (CUSTOMER)");
        };
    }

    private void seed(UserRepository repository, PasswordEncoder encoder,
                      String email, String rawPassword, String fullName, Role role) {
        if (repository.existsByEmailIgnoreCase(email)) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(encoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setRole(role);
        repository.save(user);
    }
}
