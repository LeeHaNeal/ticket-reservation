package com.ticketreservation.ticket_reservation.auth;

import com.ticketreservation.ticket_reservation.auth.dto.LoginRequest;
import com.ticketreservation.ticket_reservation.auth.dto.SignUpRequest;
import com.ticketreservation.ticket_reservation.auth.dto.TokenResponse;
import com.ticketreservation.ticket_reservation.common.exception.CustomException;
import com.ticketreservation.ticket_reservation.common.exception.ErrorCode;
import com.ticketreservation.ticket_reservation.domain.member.Member;
import com.ticketreservation.ticket_reservation.domain.member.MemberRepository;
import com.ticketreservation.ticket_reservation.domain.member.MemberRole;
import com.ticketreservation.ticket_reservation.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponse signUp(SignUpRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(MemberRole.USER)
                .build();
        memberRepository.save(member);

        return issueToken(member);
    }

    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueToken(member);
    }

    private TokenResponse issueToken(Member member) {
        String token = jwtTokenProvider.createToken(member.getEmail(), member.getRole().name());
        return TokenResponse.of(token, jwtTokenProvider.getValidityInSeconds());
    }
}
