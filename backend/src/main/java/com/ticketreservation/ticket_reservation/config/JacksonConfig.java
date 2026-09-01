package com.ticketreservation.ticket_reservation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 이 프로젝트의 Spring Boot 4.1.1 구성에서는 (원인 불명이나) Jackson
 * 자동설정이 ObjectMapper 빈을 만들어주지 않아, MVC 응답 직렬화와
 * Redis 캐시 직렬화 양쪽에 쓸 ObjectMapper를 직접 빈으로 등록한다.
 * 이 빈이 등록되면 Spring MVC의 JSON 메시지 컨버터도 이 ObjectMapper를
 * 그대로 사용한다 (Boot의 기본 설정은 @ConditionalOnMissingBean이므로).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}
