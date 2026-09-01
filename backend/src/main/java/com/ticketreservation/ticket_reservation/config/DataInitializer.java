package com.ticketreservation.ticket_reservation.config;

import com.ticketreservation.ticket_reservation.domain.member.Member;
import com.ticketreservation.ticket_reservation.domain.member.MemberRepository;
import com.ticketreservation.ticket_reservation.domain.member.MemberRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발/데모 편의를 위해 관리자 계정이 하나도 없으면 기본 관리자를 생성한다.
 * 운영 환경에서는 이 클래스를 비활성화하거나 프로필로 분리해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_EMAIL = "admin@ticket.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin1234!";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        boolean hasAdmin = memberRepository.findByEmail(DEFAULT_ADMIN_EMAIL).isPresent();
        if (hasAdmin) {
            return;
        }

        memberRepository.save(Member.builder()
                .email(DEFAULT_ADMIN_EMAIL)
                .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                .name("Admin")
                .role(MemberRole.ADMIN)
                .build());

        log.info("Default admin account created -> email: {}, password: {}",
                DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD);
    }
}
