import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [{ duration: '15s', target: 200 }, { duration: '30s', target: 200 }],
  thresholds: { http_req_duration: ['p(95)<150'], http_req_failed: ['rate<0.01'] },
};

const cities = ['Delhi', 'Mumbai', 'Bangkok'];

export default function () {
  const city = cities[Math.floor(Math.random() * cities.length)];
  const res = http.get(`http://localhost:8080/api/hotels?city=${city}&minRating=4.0`);
  check(res, { 'status 200': (r) => r.status === 200 });
}
