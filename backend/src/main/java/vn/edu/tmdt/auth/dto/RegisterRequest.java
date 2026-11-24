package vn.edu.tmdt.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email khong duoc de trong")
        @Email(message = "Email khong hop le")
        String email,

        @NotBlank(message = "Mat khau khong duoc de trong")
        @Size(min = 8, max = 72, message = "Mat khau phai tu 8 den 72 ky tu")
        String password,

        @NotBlank(message = "Ho ten khong duoc de trong")
        @Size(max = 120)
        String fullName,

        @Size(max = 20)
        String phone
) {
}
