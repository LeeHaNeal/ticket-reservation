package com.ticketreservation.ticket_reservation.common.dto;

import com.ticketreservation.ticket_reservation.common.exception.ErrorCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ErrorResponse {

    private final String code;
    private final String message;
    private final LocalDateTime timestamp = LocalDateTime.now();
    private List<FieldError> fieldErrors;

    public ErrorResponse(ErrorCode errorCode) {
        this.code = errorCode.name();
        this.message = errorCode.getMessage();
    }

    public ErrorResponse(ErrorCode errorCode, String message) {
        this.code = errorCode.name();
        this.message = message;
    }

    public ErrorResponse(ErrorCode errorCode, List<FieldError> fieldErrors) {
        this(errorCode);
        this.fieldErrors = fieldErrors;
    }

    @Getter
    public static class FieldError {
        private final String field;
        private final String reason;

        public FieldError(String field, String reason) {
            this.field = field;
            this.reason = reason;
        }
    }
}
