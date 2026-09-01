package com.ticketreservation.ticket_reservation.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long validityInSeconds;
    private final CustomUserDetailsService userDetailsService;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                             @Value("${jwt.access-token-validity-in-seconds}") long validityInSeconds,
                             CustomUserDetailsService userDetailsService) {
        this.key = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secret));
        this.validityInSeconds = validityInSeconds;
        this.userDetailsService = userDetailsService;
    }

    public String createToken(String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInSeconds * 1000);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public long getValidityInSeconds() {
        return validityInSeconds;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT: {}", e.getMessage());
        } catch (Exception e) {
            log.debug("Invalid JWT: {}", e.getMessage());
        }
        return false;
    }

    public Authentication getAuthentication(String token) {
        String email = getEmail(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getEmail(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
