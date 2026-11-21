// 카카오톡 방에 공유된 모임 링크로 참여 요청이 한꺼번에 몰리는 스파이크를 재현하는 K6 스크립트다(#129).
// 참여 요청(POST /api/v1/participants)은 X-Join-Token 헤더로 인증 없이 들어오므로 짧은 시간에 몰릴 수 있다.
// 모임은 참여자가 20명(GatheringPolicy.MAX_PARTICIPANT_COUNT)에 이르면 새 참여를 400으로 거절한다.
// 같은 모임의 참여는 모임 행 잠금으로 한 줄로 처리되므로, 몰릴 때 대기가 응답 시간에 얼마나 더해지는지와 상한이 지켜지는지를 본다.
// 스파이크 테스트는 prod와 같은 사양의 dev에서만 실행한다. 로컬 결과는 임계값의 근거로 인용하지 않는다.
// 모임 열기는 회원 JWT가 필요하다. setup에서 JWT_SECRET으로 액세스 토큰을 직접 서명한다.
// 실행 방법은 k6/README.md를 참고한다.

import http from 'k6/http';
import { check, fail } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import exec from 'k6/execution';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const JWT_SECRET = __ENV.JWT_SECRET;
// 스파이크가 정점일 때 초당 도착하는 참여 요청 수다.
const PEAK_RATE = Number(__ENV.PEAK_RATE || 200);
// 평소 초당 도착하는 참여 요청 수다.
const BASE_RATE = 5;

// GatheringPolicy.MAX_PARTICIPANT_COUNT와 같은 값이다.
const MAX_PARTICIPANT_COUNT = 20;
// 모임을 열 때 이미 상한 근처까지 채워 두는 참여자 수다. 스파이크의 처음 몇 건만 자리를 얻고 나머지는 상한에 막힌다.
const INITIAL_PARTICIPANT_COUNT = 15;
const LIMIT_EXCEEDED_CODE = 'GATHERING_PARTICIPANT_COUNT_EXCEEDS_LIMIT';
// host_member_id는 member를 FK로 참조하지 않아, 서명한 토큰의 회원 id만 있으면 모임을 열 수 있다.
const HOST_MEMBER_ID = '1';

// 상한에 막힌 400은 이 테스트가 기대하는 응답이므로 실패로 세지 않는다. setup과 teardown의 조회는 200이다.
// 그 밖의 상태 코드만 http_req_failed가 된다.
http.setResponseCallback(http.expectedStatuses(200, 201, 400));

// 스파이크가 끝난 뒤 모임장이 읽은 참여자 수가 상한 이하인지, 상한까지 채워졌는지 본다.
const participantsWithinLimit = new Rate('participants_within_limit');
const participantsAtLimit = new Rate('participants_at_limit');
// 400 응답이 상한 초과가 아닌 다른 이유로 거절된 요청이 아닌지 센다.
const rejectedForOtherReason = new Rate('rejected_for_other_reason');

export const options = {
    scenarios: {
        spike: {
            executor: 'ramping-arrival-rate',
            exec: 'join',
            startRate: BASE_RATE,
            timeUnit: '1s',
            preAllocatedVUs: 50,
            maxVUs: 400,
            stages: [
                { duration: '10s', target: BASE_RATE },
                { duration: '5s', target: PEAK_RATE },
                { duration: '30s', target: PEAK_RATE },
                { duration: '5s', target: BASE_RATE },
                { duration: '10s', target: BASE_RATE },
            ],
        },
    },
    // 임계값은 첫 dev 실행 결과를 보고 재조정한다.
    thresholds: {
        http_req_failed: ['rate<0.01'],
        'http_req_duration{expected_response:true}': ['p(95)<1000', 'p(99)<2000'],
        dropped_iterations: ['count==0'],
        participants_within_limit: ['rate==1'],
        participants_at_limit: ['rate==1'],
        rejected_for_other_reason: ['rate==0'],
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

// 모임을 열고 상한 근처까지 참여자를 채운다.
export function setup() {
    if (!JWT_SECRET) {
        fail('JWT_SECRET 환경변수가 필요하다. k6 run -e JWT_SECRET=... 로 넘긴다.');
    }

    const hostToken = signAccessToken();
    const { gatheringId, joinToken } = dataOf(
        http.post(
            `${BASE_URL}/api/v1/gatherings`,
            JSON.stringify({ name: 'K6 스파이크 모임' }),
            jsonHeaders({ Authorization: `Bearer ${hostToken}` }),
        ),
        201,
        '모임 열기',
    );
    const { consentVersion } = dataOf(
        http.get(`${BASE_URL}/api/v1/gatherings/preview`, { headers: { 'X-Join-Token': joinToken } }),
        200,
        '참여 화면 조회',
    );
    for (let sequence = 0; sequence < INITIAL_PARTICIPANT_COUNT; sequence++) {
        dataOf(joinGathering(joinToken, consentVersion, sequence), 201, '참여');
    }

    return { hostToken, gatheringId, joinToken, consentVersion };
}

export function join(data) {
    const sequence = INITIAL_PARTICIPANT_COUNT + exec.scenario.iterationInTest;
    const response = joinGathering(data.joinToken, data.consentVersion, sequence);

    check(response, {
        '참여 상태 코드는 201 또는 400이다': (res) => res.status === 201 || res.status === 400,
    });
    if (response.status === 400) {
        rejectedForOtherReason.add(response.json().data.code !== LIMIT_EXCEEDED_CODE);
    }
}

// 요청이 남은 자리보다 훨씬 많으므로, 스파이크가 끝나면 참여자는 정확히 상한이어야 한다.
export function teardown(data) {
    const participants = dataOf(
        http.get(
            `${BASE_URL}/api/v1/gatherings/${data.gatheringId}/participants`,
            jsonHeaders({ Authorization: `Bearer ${data.hostToken}` }),
        ),
        200,
        '참여자 목록 조회',
    );

    participantsWithinLimit.add(participants.length <= MAX_PARTICIPANT_COUNT);
    participantsAtLimit.add(participants.length === MAX_PARTICIPANT_COUNT);
}
