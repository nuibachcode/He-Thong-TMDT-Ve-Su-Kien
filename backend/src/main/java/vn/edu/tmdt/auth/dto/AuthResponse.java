package vn.edu.tmdt.auth.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
