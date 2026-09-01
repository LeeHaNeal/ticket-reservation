# 📌 선착순 티켓 예매 시스템

**"동시 요청 1,000건이 몰려도, 재고 100개는 정확히 100개만 팔린다"**

`Backend` `Frontend` `Database` `Concurrency` `AWS`

### 🚀 [라이브 데모 바로가기](http://3.34.11.38)

AWS EC2에 Docker Compose로 배포되어 실제로 접속해 회원가입/로그인/예매까지 체험할 수 있습니다.
> 관리자 데모 계정: `admin@ticket.com` / `admin1234!`

## ✨ 프로젝트 소개

콘서트 예매, 한정판 상품 오픈런처럼 "정해진 수량을 순서대로 정확하게" 파는 문제를
Redis 원자적 연산으로 해결한 선착순 티켓 예매 서비스입니다. DB 락 대신 Redis + Lua
스크립트로 재고 차감의 관문을 만들어 오버셀(초과 판매)을 원천 차단했고, 동시 요청
1,000건 중 정확히 재고 수량만큼만 성공하는 것을 실제 테스트로 증명했습니다.

## 🛠 사용 기술 (Tech Stack)

| 구분 | 기술 |
|---|---|
| Backend | Spring Boot 4.1, Java 17, Spring Security, JWT, JPA |
| Frontend | React 18, TypeScript, Vite, Axios, React Router |
| Database | MySQL 8, Redis 7 |
| Infra | Docker Compose, Nginx, AWS EC2 (배포), Docker Compose (MySQL/Redis 로컬 개발) |
| Version Control | Git, GitHub |

## 👥 팀원 및 역할

| 이름 | 역할 |
|---|---|
| 이하늘 | 개인 프로젝트 (기획, 백엔드 설계/구현, 프론트엔드 구현, 동시성·성능 테스트) |

## 🔥 주요 기능

✅ **회원 & 인증**
- 회원가입, 로그인 (JWT 기반)
- 권한별(USER/ADMIN) 기능 접근 제한

✅ **이벤트(티켓) 관리**
- 이벤트 등록 (관리자 전용)
- 이벤트 목록 / 상세 조회 (Redis 캐시 적용)
- 실시간 잔여 수량 조회

✅ **선착순 예매**
- Redis 원자적 연산 기반 선착순 예매 (오버셀 방지)
- 예매 취소
- 내 예매 내역 조회

✅ **동시성 / 성능 검증**
- 동시 요청 1,000건 정합성 테스트 (JUnit + `CountDownLatch`)
- k6 부하 테스트 스크립트 (처리량/응답 지연 측정)

## 💡 Problem Solving / 성과

- Redis Lua 스크립트로 재고 차감을 하나의 원자적 연산으로 묶어, DB 락 없이도
  동시 요청 1,000건에서 오버셀 0건 달성
- MySQL InnoDB 데드락 발생 원인을 로그 분석으로 추적 — "예약 INSERT와 재고 UPDATE를
  한 트랜잭션에 묶은 것"이 락 대기 사이클을 만든다는 걸 찾아 트랜잭션을 분리, 동시
  요청 실패 321건 → 0건으로 완전 해결
- Redis JSON 캐싱 시 Java record가 기본 다형성 타입 정보에서 제외되어 역직렬화가
  깨지는 문제를 `ObjectMapper` 명시적 캐싱 방식으로 해결
- 실제 동시 요청 1,000건 테스트로 정합성 수치까지 증명: **성공 100 / 매진 900 / 실패 0**

## 📂 프로젝트 구조

```
ticket-reservation/
├─ backend/
│  ├─ src/main/java/com/ticketreservation
│  ├─ src/main/resources
│  │  └─ scripts/         # reserve.lua, release.lua
│  └─ loadtest/           # k6 부하 테스트
├─ frontend/
│  ├─ src/
│  └─ package.json
└─ docker-compose.yml     # MySQL 8 + Redis 7
```

## 🌐 배포 (AWS)

EC2 단일 인스턴스에 `docker-compose.prod.yml`로 MySQL + Redis + Backend + Nginx(Frontend)
4개 컨테이너를 함께 배포했습니다. Nginx가 정적 프론트를 서빙하면서 `/api/`를 백엔드로
리버스 프록시해, 배포 환경에서는 프론트/백엔드가 같은 origin이라 별도 CORS 설정이
필요 없습니다. 배포 과정과 트러블슈팅은 [`DEPLOY.md`](./DEPLOY.md)에 정리했습니다.

## 🚀 빠른 실행 (로컬)

```bash
# 1. MySQL / Redis 기동
docker compose up -d

# 2. 백엔드 (Java 17 필요) — http://localhost:8081
cd backend && ./mvnw spring-boot:run

# 3. 프론트엔드 — http://localhost:5173
cd frontend && npm install && npm run dev
```

최초 기동 시 관리자 계정(`admin@ticket.com` / `admin1234!`)이 자동 생성됩니다.
동시성 검증 테스트만 따로 돌려보려면 `cd backend && ./mvnw test -Dtest=ReservationConcurrencyTest`.

자세한 설계 배경과 디버깅 히스토리는 [`backend/README.md`](./backend/README.md)에 정리했습니다.

---

<div align="center">

## 📊 GitHub Stats

![HaNeal's GitHub stats](https://github-readme-stats.vercel.app/api?username=LeeHaNeal&show_icons=true&theme=tokyonight)
![Top Langs](https://github-readme-stats.vercel.app/api/top-langs/?username=LeeHaNeal&layout=compact&theme=tokyonight)

</div>
