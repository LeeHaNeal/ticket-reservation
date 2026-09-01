<div align="center">

# 🎫 선착순 티켓 예매 시스템

**동시 요청 1,000건이 몰려도, 재고 100개는 정확히 100개만 팔린다.**

한정 수량 티켓을 선착순으로 예매하는 풀스택 서비스입니다. Redis 원자적 연산으로
오버셀(초과 판매)을 원천 차단하고, 실제 부하 테스트로 그 정합성을 증명했습니다.

![Java](https://img.shields.io/badge/Java_17-007396?style=flat-square&logo=openjdk&logoColor=white)
![SpringBoot](https://img.shields.io/badge/Spring_Boot_4.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![SpringSecurity](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![JPA](https://img.shields.io/badge/JPA-59666C?style=flat-square&logo=hibernate&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL_8-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis_7-DC382D?style=flat-square&logo=redis&logoColor=white)
![React](https://img.shields.io/badge/React_18-61DAFB?style=flat-square&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?style=flat-square&logo=docker&logoColor=white)

</div>

<br>

## 왜 만들었나

콘서트 티켓, 한정판 상품 오픈런처럼 "정해진 수량을 순서대로 정확하게" 파는 문제는
백엔드에서 흔히 나오지만 제대로 다루기 까다로운 주제입니다. 이 프로젝트는 그 문제
하나를 정면으로 붙잡고, **동시성 제어 · 캐싱 · 인증 · 성능 검증**까지 실제로 눈으로
확인 가능한 형태로 만든 포트폴리오입니다.

## 핵심: 오버셀 없는 선착순 재고 차감

재고 차감의 "관문"은 DB 락이 아니라 **Redis + Lua 스크립트**가 담당합니다.

```
event:{id}:stock    -> 남은 재고 (정수 카운터)
event:{id}:members  -> 이미 예매한 회원 ID 집합 (중복 예매 방지)
```

`reserve.lua`가 "중복 예매 확인 → 재고 확인 → 차감 → 예매자 등록"을 하나의 원자적
연산으로 처리합니다. Redis는 싱글 스레드로 명령을 순차 실행하므로, 수천 개의 요청이
동시에 들어와도 이 스크립트 내부에서는 경쟁 상태(race condition)가 생기지 않습니다.

`SELECT ... FOR UPDATE` 같은 DB 비관적 락 대신 이 방식을 택한 이유는, 락은 커넥션을
잡고 순서대로 대기시키는 방식이라 트래픽이 몰릴수록 커넥션 풀이 고갈되고 처리량이
급격히 떨어지기 때문입니다. Redis 원자적 연산은 커넥션을 점유하지 않고 인메모리에서
즉시 승인/거절을 판정해 훨씬 높은 처리량을 냅니다.

Redis 승인 이후 DB 저장이 실패하면 `release.lua`로 즉시 보상(compensate)해 Redis
상태를 되돌리고, DB에도 `(member_id, event_id)` 유니크 제약을 걸어 두어 어떤 이유로든
이중 예매가 실제로 저장되는 일은 없도록 이중 안전망을 뒀습니다.

## 실제로 증명한 결과

`ReservationConcurrencyTest`는 재고 100개짜리 이벤트에 서로 다른 회원 1,000명이
`CountDownLatch`로 완전히 동시에 예매를 시도하는 상황을 재현합니다.

```
성공: 100, 매진 실패: 900, 기타 실패: 0
Tests run: 1, Failures: 0, Errors: 0 — BUILD SUCCESS
```

처음부터 한 번에 통과한 건 아닙니다. 초기 실행에서는 MySQL InnoDB 데드락으로
동시 요청 중 최대 321건이 실패했고, 원인을 로그로 추적해 "예약 INSERT와 재고 UPDATE를
하나의 트랜잭션으로 묶은 것"이 락 대기 사이클을 만든다는 걸 찾아 두 개의 독립된
트랜잭션으로 분리하고서야 완전히 해결했습니다. 자세한 디버깅 과정은
[`backend/README.md`](./backend/README.md)에 정리해 뒀습니다.

## 프로젝트 구조

```
ticket-reservation/
├── backend/    Spring Boot API 서버 (Java 17, MySQL, Redis)
├── frontend/   React + TypeScript 클라이언트 (Vite)
└── docker-compose.yml   MySQL 8 + Redis 7
```

## 빠른 실행

```bash
# 1. MySQL / Redis 기동
docker compose up -d

# 2. 백엔드 (Java 17 필요) — http://localhost:8081
cd backend
./mvnw spring-boot:run

# 3. 프론트엔드 — http://localhost:5173
cd frontend
npm install
npm run dev
```

최초 기동 시 관리자 계정(`admin@ticket.com` / `admin1234!`)이 자동 생성됩니다.

동시성 검증 테스트만 따로 돌려보려면:

```bash
cd backend
./mvnw test -Dtest=ReservationConcurrencyTest
```

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

## 프론트엔드

React + TypeScript + Vite. JWT는 axios 인터셉터로 모든 요청에 자동 첨부되고,
`react-router-dom`의 라우트 가드로 로그인/관리자 권한이 필요한 페이지를 보호합니다.
이벤트 상세 페이지는 3초 간격으로 잔여 수량을 폴링해 실시간에 가깝게 보여줍니다.

- 회원가입 / 로그인
- 이벤트 목록 / 상세 (실시간 잔여 수량)
- 선착순 예매 / 취소, 내 예매 내역
- 관리자: 이벤트(티켓) 등록

## 성능 테스트 (k6)

`backend/loadtest/reserve.js`는 서로 다른 사용자 N명이 동시에 하나의 이벤트를
예매하는 "오픈런" 상황을 재현해 처리량(RPS), 응답 지연(p95), 매진 처리 정확도를
함께 측정합니다.

```bash
k6 run -e EVENT_ID=1 -e VUS=500 backend/loadtest/reserve.js
```

## 향후 개선 아이디어

- DB 저장을 Kafka/RabbitMQ로 비동기화해 응답 지연 더 낮추기
- 좌석 단위(수량이 아닌 좌석 지정) 예매로 확장
- Refresh Token / 토큰 재발급
- Redisson 분산락 버전을 별도 브랜치로 구현해 Lua 스크립트 방식과 처리량 비교
- Spring Cloud Gateway + Rate Limiting으로 트래픽 스파이크 방어
- AWS 배포 (ECS/EKS + ElastiCache + RDS), GitHub Actions CI/CD
