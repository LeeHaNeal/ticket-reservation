// k6 부하 테스트: 선착순 예매 API 성능/정합성 측정
//
// 설치: https://k6.io/docs/get-started/installation/
// 실행 전: 애플리케이션이 8081 포트로 떠 있어야 하고, 아래 EVENT_ID의 이벤트가
//          k6 실행 시점 기준으로 예매 오픈 상태여야 한다.
//
// 사용법:
//   1) 관리자로 로그인해 재고 N개짜리 이벤트를 하나 생성하고 EVENT_ID를 채운다.
//   2) k6 run -e EVENT_ID=1 -e VUS=500 loadtest/reserve.js
//
// 이 스크립트는 각 가상유저(VU)마다 서로 다른 회원을 회원가입시켜 토큰을 받은 뒤,
// 정확히 한 번씩 "동시에" 예매를 시도한다. 재고보다 VU 수가 많아야 매진 상황을
// 재현해 오버셀 여부와 처리량(RPS)/응답 지연을 함께 확인할 수 있다.

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const EVENT_ID = __ENV.EVENT_ID || '1';

const successCount = new Counter('reservation_success');
const soldOutCount = new Counter('reservation_sold_out');
const otherFailureCount = new Counter('reservation_other_failure');
const reserveDuration = new Trend('reserve_duration', true);

export const options = {
    scenarios: {
        fcfs_rush: {
            executor: 'per-vu-iterations',
            vus: Number(__ENV.VUS || 300),
            iterations: 1,
            maxDuration: '60s',
        },
    },
    thresholds: {
        reserve_duration: ['p(95)<1000'],
    },
};

export default function () {
    const email = `loadtest-${__VU}-${Date.now()}@example.com`;
    const signupRes = http.post(`${BASE_URL}/api/auth/signup`, JSON.stringify({
        email,
        password: 'loadtest1234',
        name: `vu-${__VU}`,
    }), { headers: { 'Content-Type': 'application/json' } });

    check(signupRes, { 'signup succeeded': (r) => r.status === 201 });
    const token = signupRes.json('accessToken');

    const reserveRes = http.post(
        `${BASE_URL}/api/events/${EVENT_ID}/reservations`,
        null,
        { headers: { Authorization: `Bearer ${token}` } }
    );

    reserveDuration.add(reserveRes.timings.duration);

    if (reserveRes.status === 201) {
        successCount.add(1);
    } else if (reserveRes.status === 409 && reserveRes.json('code') === 'SOLD_OUT') {
        soldOutCount.add(1);
    } else {
        otherFailureCount.add(1);
        console.error(`unexpected response: ${reserveRes.status} ${reserveRes.body}`);
    }
}
