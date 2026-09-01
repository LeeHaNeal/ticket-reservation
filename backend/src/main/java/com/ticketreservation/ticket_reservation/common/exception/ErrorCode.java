package com.ticketreservation.ticket_reservation.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Auth / Member
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 토큰입니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // Event
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 이벤트입니다."),
    INVALID_EVENT_PERIOD(HttpStatus.BAD_REQUEST, "예매 시작 시각은 종료 시각보다 이전이어야 합니다."),

    // Reservation
    SOLD_OUT(HttpStatus.CONFLICT, "티켓이 모두 소진되었습니다."),
    ALREADY_RESERVED(HttpStatus.CONFLICT, "이미 해당 이벤트를 예매했습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 예매입니다."),
    RESERVATION_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 예매입니다."),
    RESERVATION_NOT_OPEN(HttpStatus.CONFLICT, "예매 가능 시간이 아닙니다."),
    RESERVATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "예매 처리 중 오류가 발생했습니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
