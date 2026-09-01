# AWS EC2 배포 가이드

EC2 인스턴스 한 대에 Docker Compose로 MySQL + Redis + 백엔드 + 프론트엔드를 전부
띄우는 방식입니다. 프리티어 인스턴스로도 충분히 돌아갑니다.

## 1. EC2 인스턴스 생성

1. AWS 콘솔 → EC2 → **인스턴스 시작**
2. AMI: **Ubuntu Server 22.04 LTS** (프리티어 사용 가능)
3. 인스턴스 유형: **t2.micro** 또는 **t3.micro** (프리티어 표시된 걸로)
4. 키 페어: 새로 생성 → 이름 정하고 **.pem 파일 다운로드** (분실하면 재발급 불가하니 잘 보관)
5. 네트워크 설정 → 보안 그룹 인바운드 규칙:
   - SSH (22) — 소스: 내 IP (권장) 또는 위치 무관
   - HTTP (80) — 소스: 위치 무관 (0.0.0.0/0)
   - **3306(MySQL), 6379(Redis)는 절대 열지 마세요** — 컨테이너 내부 네트워크로만
     통신하므로 외부에 열 필요가 없고, 열면 보안 위험만 커집니다.
6. 스토리지: 기본값(8GB) 그대로 두면 됩니다.
7. **인스턴스 시작** → 몇 분 기다렸다가 인스턴스 목록에서 **퍼블릭 IPv4 주소** 확인.

## 2. SSH 접속

Windows PowerShell 또는 Git Bash에서 (.pem 파일 있는 경로로 이동 후):

```bash
chmod 400 그키페어이름.pem
ssh -i 그키페어이름.pem ubuntu@퍼블릭IP주소
```

## 3. Docker 설치 (EC2 안에서)

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker
```

## 4. 코드 가져오기

```bash
git clone https://github.com/LeeHaNeal/ticket-reservation.git
cd ticket-reservation
```

## 5. 환경변수(시크릿) 설정

```bash
cp .env.prod.example .env.prod
nano .env.prod
```

`MYSQL_ROOT_PASSWORD`, `JWT_SECRET`을 각각 강력한 랜덤 값으로 바꿔주세요. 아래
명령으로 생성한 값을 붙여넣으면 됩니다.

```bash
openssl rand -base64 24   # MYSQL_ROOT_PASSWORD 용
openssl rand -base64 48   # JWT_SECRET 용
```

## 6. 빌드 및 실행

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

처음 빌드는 몇 분 걸립니다. 완료되면:

```bash
docker compose -f docker-compose.prod.yml ps
```

4개 컨테이너(ticket-mysql, ticket-redis, ticket-backend, ticket-frontend)가 모두
`Up` 상태인지 확인하세요.

## 7. 접속 확인

브라우저에서 `http://퍼블릭IP주소` 로 접속하면 프론트엔드 화면이 뜹니다. 회원가입 →
로그인 → 이벤트 조회까지 로컬에서 하던 것과 동일하게 동작해야 합니다.

관리자 계정은 최초 기동 시 자동 생성됩니다: `admin@ticket.com` / `admin1234!`
(포트폴리오 데모용이니 실사용 서비스라면 반드시 바꿔야 합니다).

## 8. 이후 코드 업데이트 시

```bash
cd ticket-reservation
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

## 9. 문제 생기면

```bash
docker compose -f docker-compose.prod.yml logs -f backend   # 백엔드 로그
docker compose -f docker-compose.prod.yml logs -f frontend  # nginx 로그
```

## 비용 참고

t2.micro/t3.micro는 AWS 프리티어(가입 후 12개월, 월 750시간)로 커버되는 경우가
많습니다. 프리티어가 끝났거나 대상이 아니라면 24시간 켜뒀을 때 월 대략 $7~10
수준입니다 (리전/인스턴스 유형에 따라 다름). 스토리지(EBS)도 소액 과금됩니다.
비용이 걱정되면 AWS 콘솔의 **Billing → Budgets**에서 예산 알림을 걸어두는 걸
추천합니다.
