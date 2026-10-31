// gathering 도메인에서 모임 참여(POST /api/v1/participants)와 분담금 조회(GET /api/v1/shares)를 동시에 요청하는 K6 스크립트다.
// 두 API 모두 모임을 읽을 때 참여자 전원의 이름과 휴대폰을 복호화하므로, 참여자가 늘어나는 동안의 응답 시간을 측정한다(#125).
// 모임 열기는 회원 JWT가 필요하다. 로그인은 소셜 인가 코드가 있어야 해서 setup에서 JWT_SECRET으로 액세스 토큰을 직접 서명한다.
// 실행 방법은 k6/README.md를 참고한다.

import http from 'k6/http';
import { check, fail } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const JWT_SECRET = __ENV.JWT_SECRET;

// 모임 하나에 처음부터 있는 참여자 수다. 한 모임의 참여자는 수십 명 수준이라는 #124 결정의 전제를 따른다.
const INITIAL_PARTICIPANT_COUNT = 30;
// 측정하는 동안 같은 모임에 더 참여하는 인원이다. 참여가 끝나면 모임의 참여자는 80명이 된다.
const JOIN_COUNT = 50;
// host_member_id는 member를 FK로 참조하지 않아, 서명한 토큰의 회원 id만 있으면 모임을 열 수 있다.
const HOST_MEMBER_ID = '1';

export const options = {
    scenarios: {
        // 참여를 1분 동안 고르게 나눠, 분담금 조회가 모두 참여자가 늘어나는 동안 일어나게 한다.
        join: {
            executor: 'constant-arrival-rate',
            exec: 'join',
            rate: JOIN_COUNT,
            timeUnit: '1m',
            duration: '1m',
            preAllocatedVUs: 5,
        },
        shares: {
            executor: 'constant-vus',
            exec: 'getShares',
            vus: 20,
            duration: '1m',
        },
    },
    thresholds: {
        'http_req_failed{scenario:join}': ['rate<0.01'],
        'http_req_failed{scenario:shares}': ['rate<0.01'],
        'http_req_duration{scenario:join}': ['p(95)<500'],
        'dropped_iterations{scenario:join}': ['count==0'],
        'http_req_duration{scenario:shares}': ['p(95)<300', 'p(99)<800'],
        'checks{scenario:shares}': ['rate==1'],
    },
};

function signAccessToken() {
    const header = encoding.b64encode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }), 'rawurl');
    const now = Math.floor(Date.now() / 1000);
    const payload = encoding.b64encode(
        JSON.stringify({ sub: HOST_MEMBER_ID, role: 'ROLE_MEMBER', iat: now, exp: now + 3600 }),
        'rawurl',
    );
    const signature = crypto.hmac('sha256', JWT_SECRET, `${header}.${payload}`, 'base64rawurl');
    return `${header}.${payload}.${signature}`;
}

function jsonHeaders(extraHeaders) {
    return { headers: Object.assign({ 'Content-Type': 'application/json' }, extraHeaders) };
}

function dataOf(response, expectedStatus, step) {
    if (response.status !== expectedStatus) {
        fail(`${step} 실패: ${response.status} ${response.body}`);
    }
    return response.json().data;
}

// 휴대폰은 모임 안에서 중복되지 않아야 하므로 순번으로 번호를 만든다.
function phoneNumberOf(sequence) {
    return `010${String(sequence).padStart(8, '0')}`;
}

function joinGathering(joinToken, consentVersion, sequence) {
    return http.post(
        `${BASE_URL}/api/v1/participants`,
        JSON.stringify({
            name: `참여자${sequence}`,
            phoneNumber: phoneNumberOf(sequence),
            consentAgreed: true,
            consentVersion,
        }),
        jsonHeaders({ 'X-Join-Token': joinToken }),
    );
}

// 모임을 열고 참여자를 채운 뒤, 첫 참여자가 결제자로 계좌를 등록하고 참여자 전원의 정산 건을 기록한다.
export function setup() {
    if (!JWT_SECRET) {
        fail('JWT_SECRET 환경변수가 필요하다. k6 run -e JWT_SECRET=... 로 넘긴다.');
    }

    const { joinToken } = dataOf(
        http.post(
            `${BASE_URL}/api/v1/gatherings`,
            JSON.stringify({ name: 'K6 측정 모임' }),
            jsonHeaders({ Authorization: `Bearer ${signAccessToken()}` }),
        ),
        201,
        '모임 열기',
    );
    const { consentVersion } = dataOf(
        http.get(`${BASE_URL}/api/v1/gatherings/preview`, { headers: { 'X-Join-Token': joinToken } }),
        200,
        '참여 화면 조회',
    );

    const participants = [];
    for (let sequence = 0; sequence < INITIAL_PARTICIPANT_COUNT; sequence++) {
        participants.push(dataOf(joinGathering(joinToken, consentVersion, sequence), 201, '참여'));
    }

    const payerLinkToken = participants[0].linkToken;
    const form = dataOf(
        http.get(`${BASE_URL}/api/v1/settlements/form`, { headers: { 'X-Link-Token': payerLinkToken } }),
        200,
        '기록 화면 조회',
    );
    dataOf(
        http.post(
            `${BASE_URL}/api/v1/participants/bank-account`,
            JSON.stringify({
                bankName: '국민은행',
                accountNumber: '12345678901234',
                consentAgreed: true,
                consentVersion: form.bankAccountConsentVersion,
            }),
            jsonHeaders({ 'X-Link-Token': payerLinkToken }),
        ),
        201,
        '계좌 등록',
    );
    dataOf(
        http.post(
            `${BASE_URL}/api/v1/settlements`,
            JSON.stringify({
                totalAmount: 30 * 31250,
                participantIds: participants.map((participant) => participant.id),
                fixedAmounts: [],
            }),
            jsonHeaders({ 'X-Link-Token': payerLinkToken }),
        ),
        201,
        '정산 건 기록',
    );

    return {
        joinToken,
        consentVersion,
        // 결제자는 보낼 분담금이 없으므로 결제자를 뺀 참여자의 링크 토큰으로 조회한다.
        linkTokens: participants.slice(1).map((participant) => participant.linkToken),
    };
}

export function join(data) {
    const sequence = INITIAL_PARTICIPANT_COUNT + exec.scenario.iterationInTest;
    const response = joinGathering(data.joinToken, data.consentVersion, sequence);

    check(response, {
        '참여 상태 코드는 201이다': (res) => res.status === 201,
    });
}

export function getShares(data) {
    const linkToken = data.linkTokens[Math.floor(Math.random() * data.linkTokens.length)];
    const response = http.get(`${BASE_URL}/api/v1/shares`, { headers: { 'X-Link-Token': linkToken } });

    check(response, {
        '분담금 조회 상태 코드는 200이다': (res) => res.status === 200,
        '결제자 묶음이 하나다': (res) => res.status === 200 && res.json().data.length === 1,
    });
}
