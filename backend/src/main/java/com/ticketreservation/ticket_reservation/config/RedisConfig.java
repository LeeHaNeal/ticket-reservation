package com.ticketreservation.ticket_reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisConfig {

    /** 선착순 재고 차감을 원자적으로 수행하는 Lua 스크립트. */
    @Bean
    public DefaultRedisScript<Long> reserveStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/reserve.lua"));
        script.setResultType(Long.class);
        return script;
    }

    /** 예매 취소/보상 트랜잭션에서 재고를 원자적으로 되돌리는 Lua 스크립트. */
    @Bean
    public DefaultRedisScript<Long> releaseStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/release.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
