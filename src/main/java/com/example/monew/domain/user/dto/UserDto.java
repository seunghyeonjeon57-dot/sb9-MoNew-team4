package com.example.monew.domain.user.dto;

import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
    UUID id,
    String email,
    String nickname,
    LocalDateTime createdAt
) {

}
