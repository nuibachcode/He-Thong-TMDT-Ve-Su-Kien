package vn.edu.tmdt.auth.dto;

public record UserResponse(Long id, String email, String fullName, String phone, String role) {
}
