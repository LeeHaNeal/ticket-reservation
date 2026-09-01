package com.ticketreservation.ticket_reservation.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.") String password,
        @NotBlank @Size(max = 50) String name
) {
}
