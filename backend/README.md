# 선착순 티켓 예매 시스템 (Ticket Reservation Backend)

신입 백엔드 개발자 포트폴리오용 프로젝트. 한정 수량 티켓을 선착순으로 예매하는
백엔드 API 서버로, **동시성 제어**, **캐싱**, **성능 테스트**를 중심으로 설계했다.

## 기술 스택

- Java 17, Spring Boot 4.1.1
- Spring Security (JWT 인증)
- Spring Data JPA + MySQL 8
- Spring Data Redis (Lettuce) + Lua Script
- Maven
- k6 (부하 테스트)

## 핵심 설계: 오버셀(초과 판매) 없는 선착순 재고 차감

재고 차감의 "관문(gate)" 은 **Redis + Lua 스크립트**가 담당한다.

```
event:{id}:stock    -> 남은 재고 수 (정수 카운터)
event:{id}:members  -> 이미 예매한 회원 ID 집합 (SET, 중복 예매 방지)
```

`reserve.lua`는 "이미 예매했는지 확인 -> 재고 확인 -> 차감 -> 예매자 등록"을 하나의
원자적 연산으로 처리한다. Redis는 싱글 스레드로 명령을 순차 실행하기 때문에, 수천
개의 요청이 동시에 들어와도 이 스크립트 내부에서는 경쟁 상태(race condition)가
발생하지 않는다.

**왜 DB 비관적 락(`SELECT ... FOR UPDATE`) 대신 이 방식을 택했는가?**
비관적 락은 커넥션을 잡고 순서대로 대기시키는 방식이라 트래픽이 몰릴수록 커넥션
풀이 고갈되고 처리량(TPS)이 급격히 떨어진다. Redis 원자적 연산은 커넥션을 점유하지
않고 인메모리에서 즉시 승인/거절을 판정하므로 훨씬 높은 처리량을 낼 수 있다 -
실제 티켓 오픈런 트래픽 처리에 널리 쓰이는 패턴이다.

**Redis 승인 이후 DB 저장이 실패하면?**
`release.lua`로 즉시 보상(compensate)해서 Redis 상태를 되돌린다. 또한 DB에도
`(member_id, event_id)` 유니크 제약을 걸어 두어, Redis와 DB가 어떤 이유로든 어긋나도
이중 예매가 실제로 저장되는 일은 없다 (안전망 이중화).

이벤트 상세 조회(`GET /api/events/{id}`)는 이것과 별개로 **캐시-어사이드(cache-aside)**
패턴으로 Redis에 10분 TTL로 캐싱한다 - 읽기 부하 완화가 목적인 일반적인 캐싱과,
쓰기 경합을 원자적으로 제어하는 재고 카운터는 성격이 다르므로 의도적으로 분리했다.

## 동시성 정합성 검증 테스트

`ReservationConcurrencyTest`는 재고 100개짜리 이벤트에 서로 다른 회원 1000명이
`CountDownLatch`로 동시에 예매를 시도하는 상황을 재현하고, 정확히 100건만 성공하고
나머지는 매진(SOLD_OUT)으로 실패하는지 검증한다.

```bash
docker compose up -d
./mvnw test -Dtest=ReservationConcurrencyTest
```

## 로컬 실행

```bash
# 1. MySQL / Redis 기동
docker compose up -d

# 2. 애플리케이션 실행 (Java 17 필요)
./mvnw spring-boot:run
```

서버는 `http://localhost:8081` 에서 뜬다. 최초 기동 시 관리자 계정
(`admin@ticket.com` / `admin1234!`)이 자동 생성된다 (`DataInitializer`).

## API 요약

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/auth/signup` | - | 회원가입 |
| POST | `/api/auth/login` | - | 로그인, JWT 발급 |
| POST | `/api/events` | ADMIN | 이벤트(티켓) 생성 |
| GET | `/api/events` | - | 이벤트 목록 조회 |
| GET | `/api/events/{id}` | - | 이벤트 상세 조회 (캐시) |
| GET | `/api/events/{id}/stock` | - | 실시간 잔여 수량 조회 |
| POST | `/api/events/{id}/reservations` | USER | 선착순 예매 |
| DELETE | `/api/reservations/{id}` | USER | 예매 취소 |
| GET | `/api/reservations/me` | USER | 내 예매 목록 |

### curl 예시

```bash
# 회원가입
curl -s -X POST http://localhost:8081/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"user1@example.com","password":"password1234","name":"홍길동"}'

# 로그인 (관리자)
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ticket.com","password":"admin1234!"}' | jq -r .accessToken)

# 이벤트 생성 (100장 한정, 지금부터 1시간 예매 오픈)
curl -s -X POST http://localhost:8081/api/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "2026 콘서트",
    "description": "한정판 티켓",
    "venue": "잠실 올림픽 경기장",
    "totalStock": 100,
    "reservationStartAt": "2026-09-01T00:00:00",
    "reservationEndAt": "2026-12-31T23:59:59"
  }'

# 예매 (일반 사용자 토큰으로)
curl -s -X POST http://localhost:8081/api/events/1/reservations \
  -H "Authorization: Bearer $USER_TOKEN"
```

## 성능 테스트 (k6)

`loadtest/reserve.js`는 서로 다른 사용자 N명이 동시에 하나의 이벤트를 예매하는
"오픈런" 상황을 재현해 처리량(RPS)과 응답 지연(p95), 그리고 매진 처리 정확도를
함께 측정한다.

```bash
# k6 설치 후
k6 run -e EVENT_ID=1 -e VUS=500 loadtest/reserve.js
```

## 향후 개선 아이디어 (포트폴리오 확장 포인트)

- DB 저장을 동기 처리 대신 Kafka/RabbitMQ로 비동기화해 응답 지연을 더 낮추기
- 좌석 단위(수량이 아닌 좌석 지정) 예매로 확장
- Refresh Token / 토큰 재발급
- Redisson 분산락 버전을 별도 브랜치로 구현해 Lua 스크립트 방식과 처리량 비교 (JMH/k6)
- Spring Cloud Gateway + Rate Limiting으로 트래픽 스파이크 방어
- AWS 배포 (ECS/EKS + ElastiCache + RDS), GitHub Actions CI/CD
- React + TypeScript 프론트엔드 연동
