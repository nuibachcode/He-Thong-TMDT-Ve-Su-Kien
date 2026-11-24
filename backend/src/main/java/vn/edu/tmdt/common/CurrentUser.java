package vn.edu.tmdt.common;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import vn.edu.tmdt.auth.User;
import vn.edu.tmdt.auth.UserRepository;
import vn.edu.tmdt.common.exception.AppException;

@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User require(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw AppException.unauthorized("Ban can dang nhap");
        }
        return users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> AppException.unauthorized("Phien dang nhap khong hop le"));
    }
}
