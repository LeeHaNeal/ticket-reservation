package com.ticketreservation.ticket_reservation.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketreservation.ticket_reservation.common.exception.CustomException;
import com.ticketreservation.ticket_reservation.common.exception.ErrorCode;
import com.ticketreservation.ticket_reservation.domain.event.Event;
import com.ticketreservation.ticket_reservation.domain.event.EventRepository;
import com.ticketreservation.ticket_reservation.event.dto.EventCreateRequest;
import com.ticketreservation.ticket_reservation.event.dto.EventResponse;
import com.ticketreservation.ticket_reservation.event.dto.StockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

/**
 * 이벤트 조회는 읽기 위주(read-heavy)이므로 캐시-어사이드(cache-aside) 패턴으로
 * Redis에 상세 정보를 JSON 문자열로 캐싱한다. 반면 "잔여 수량"은 쓰기 경합이 극심한
 * 값이라 ReservationService가 별도의 Lua 스크립트로 다루는 카운터(event:{id}:stock)를
 * 사용한다 - 이 둘은 캐싱 목적이 다르므로 의도적으로 분리했다.
 *
 * 캐시 값은 (Generic 타입 정보에 의존하는 직렬화기 대신) ObjectMapper로 직접
 * 문자열 <-> DTO 변환을 해서 StringRedisTemplate 하나로 모든 Redis 값을 다룬다 -
 * record(=final 클래스)는 다형성 타입 메타데이터 없이 저장되므로 이렇게 명시적으로
 * 타입을 지정해 역직렬화하는 편이 더 안전하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final EventRepository eventRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public EventResponse createEvent(EventCreateRequest request) {
        if (request.reservationStartAt().isAfter(request.reservationEndAt())) {
            throw new CustomException(ErrorCode.INVALID_EVENT_PERIOD);
        }

        Event event = Event.builder()
                .name(request.name())
                .description(request.description())
                .venue(request.venue())
                .totalStock(request.totalStock())
                .reservationStartAt(request.reservationStartAt())
                .reservationEndAt(request.reservationEndAt())
                .build();
        eventRepository.save(event);

        // Redis를 재고 카운터의 source of truth로 초기화한다.
        redisTemplate.opsForValue().set(event.stockRedisKey(), String.valueOf(event.getTotalStock()));

        EventResponse response = EventResponse.from(event);
        cachePut(event.cacheKey(), response);
        return response;
    }

    public EventResponse getEvent(Long eventId) {
        String cacheKey = "event:" + eventId + ":info";

        Optional<EventResponse> cached = cacheGet(cacheKey);
        if (cached.isPresent()) {
            log.debug("cache hit for {}", cacheKey);
            return cached.get();
        }

        Event event = findEventOrThrow(eventId);
        EventResponse response = EventResponse.from(event);
        cachePut(cacheKey, response);
        return response;
    }

    public Page<EventResponse> listEvents(Pageable pageable) {
        return eventRepository.findAll(pageable).map(EventResponse::from);
    }

    public StockResponse getRemainingStock(Long eventId) {
        Event event = findEventOrThrow(eventId);
        String cached = redisTemplate.opsForValue().get(event.stockRedisKey());

        if (cached != null) {
            int stock = Integer.parseInt(cached);
            return new StockResponse(eventId, Math.max(stock, 0), "redis");
        }

        // Redis 캐시 미스(콜드 스타트/장애 복구) 시 DB 값을 기준으로 재적재한다.
        redisTemplate.opsForValue().set(event.stockRedisKey(), String.valueOf(event.getRemainingStock()));
        return new StockResponse(eventId, event.getRemainingStock(), "db-fallback");
    }

    Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(ErrorCode.EVENT_NOT_FOUND));
    }

    private void cachePut(String key, EventResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize event response for cache key {}", key, e);
        }
    }

    private Optional<EventResponse> cacheGet(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, EventResponse.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached event for key {}, evicting", key, e);
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }
}
