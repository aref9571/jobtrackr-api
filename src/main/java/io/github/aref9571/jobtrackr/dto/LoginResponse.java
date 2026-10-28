package io.github.aref9571.jobtrackr.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
