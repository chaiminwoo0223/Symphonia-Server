// pairing 도메인 GET /api/v1/pairings/recommend의 로컬/dev 성능 기준선을 측정하는 K6 스크립트다.
// 인증이 필요 없는 permitAll 엔드포인트라 별도 토큰 발급 없이 바로 호출한다.
// 실행 방법은 loadtest/README.md를 참고한다.

import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const RELATIONSHIP_TYPES = ['BOSS', 'FRIEND', 'COLLEAGUE', 'LOVER'];
const MOOD_TYPES = ['FORMAL', 'CASUAL', 'ROMANTIC', 'CELEBRATORY'];
const ATTENDEE_CONSTRAINTS = ['DRIVER', 'PREGNANT', 'NON_DRINKER'];
const ALLERGY_TYPES = [
    'EGG',
    'MILK',
    'PEANUT',
    'WALNUT',
    'PINE_NUT',
    'SHELLFISH',
    'SHRIMP',
    'CRAB',
    'SQUID',
    'MACKEREL',
    'PORK',
    'BEEF',
    'CHICKEN',
    'SOYBEAN',
    'WHEAT',
    'BUCKWHEAT',
    'PEACH',
    'TOMATO',
    'SULFITE',
];

// 응답이 200이면서도 추천 결과가 비어 있는 비율을 추적한다.
// 알레르기 조합이 과하게 겹쳐 카탈로그를 모두 걸러내는 시나리오가 섞여 있는지 확인하기 위해서다.
const emptyRecommendationRate = new Rate('empty_recommendation_rate');

export const options = {
    scenarios: {
        baseline: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 },
                { duration: '1m', target: 20 },
                { duration: '30s', target: 0 },
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<300', 'p(99)<800'],
        empty_recommendation_rate: ['rate<0.05'],
    },
};

function pickOne(values) {
    return values[Math.floor(Math.random() * values.length)];
}

// 0개부터 maxCount개까지 중복 없이 무작위로 고른다. Fisher-Yates로 섞어 정렬 기반 셔플의 분포 편향을 피한다.
function pickSome(values, maxCount) {
    const count = Math.floor(Math.random() * (maxCount + 1));
    const shuffled = [...values];
    for (let i = shuffled.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    return shuffled.slice(0, count);
}

function buildQueryString() {
    const params = [];
    params.push(`relationshipType=${pickOne(RELATIONSHIP_TYPES)}`);
    params.push(`moodType=${pickOne(MOOD_TYPES)}`);
    params.push('isAdultConfirmed=true');
    pickSome(ATTENDEE_CONSTRAINTS, 2).forEach((value) =>
        params.push(`attendeeConstraints=${value}`),
    );
    pickSome(ALLERGY_TYPES, 2).forEach((value) =>
        params.push(`attendeeAllergies=${value}`),
    );
    return params.join('&');
}

// 200이 아니거나 바디가 JSON이 아닌 응답에서 res.json()이 던지는 예외로 이터레이션이 중단되지 않도록,
// 파싱을 먼저 한 번만 시도하고 그 결과를 check에서 재사용한다.
function parseBody(response) {
    if (response.status !== 200) {
        return null;
    }
    try {
        return response.json();
    } catch {
        return null;
    }
}

export default function () {
    const response = http.get(`${BASE_URL}/api/v1/pairings/recommend?${buildQueryString()}`);
    const body = parseBody(response);

    const isSuccess = check(response, {
        '상태 코드는 200이다': (res) => res.status === 200,
        '응답 바디는 ok:true다': () => body !== null && body.ok === true,
        'data는 배열이다': () => body !== null && Array.isArray(body.data),
    });

    if (isSuccess) {
        emptyRecommendationRate.add(body.data.length === 0);
    }
}
