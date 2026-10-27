package io.github.aref9571.jobtrackr.dto;

import io.github.aref9571.jobtrackr.model.User;

import java.util.UUID;

public record RegisterResponse(UUID id, String email) {

    public static RegisterResponse from(User user) {
        return new RegisterResponse(user.getId(), user.getEmail());
    }
}
