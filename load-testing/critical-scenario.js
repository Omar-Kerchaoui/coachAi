// k6 load test against the critical scenario (docs/TESTING.md §2,
// docs/backlog L10-1010): register -> login -> onboard (create goal, log
// weight) -> a signed WhatsApp webhook turn -> read subscription status.
//
// STATUS: written, never executed. k6 isn't installed in the environment
// this was authored in, and there's no staging deployment yet to point it
// at (see infra/README.md) — running this against localhost would only
// prove the script itself is well-formed, not anything about real launch
// volume. Documented here rather than fabricating results.
//
// Usage once a target environment exists:
//   k6 run -e BASE_URL=https://api-staging.lose10.app -e WHATSAPP_APP_SECRET=... critical-scenario.js
//
// Tune stages() below to the actual target from docs/backlog L10-1010's
// AC ("documented results against a defined target — concurrent users,
// messages/sec, AI call latency under load") once that target is decided;
// the numbers here are a starting-point guess, not a validated target.

import http from "k6/http";
import { check, sleep } from "k6";
import crypto from "k6/crypto";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const WHATSAPP_APP_SECRET = __ENV.WHATSAPP_APP_SECRET || "local-dev-test-webhook-secret";

export const options = {
  stages: [
    { duration: "1m", target: 20 },  // ramp-up
    { duration: "3m", target: 20 },  // steady state — 20 concurrent users
    { duration: "1m", target: 50 },  // spike
    { duration: "2m", target: 50 },
    { duration: "1m", target: 0 },   // ramp-down
  ],
  thresholds: {
    http_req_duration: ["p(95)<1000"], // 95% of requests under 1s
    http_req_failed: ["rate<0.01"],    // error rate under 1%
  },
};

function hmacSha256Hex(secret, body) {
  return crypto.hmac("sha256", secret, body, "hex");
}

export default function () {
  const email = `k6-${__VU}-${__ITER}-${Date.now()}@example.com`;
  const password = "LoadTest123!";

  const registerRes = http.post(
    `${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({ email, password }),
    { headers: { "Content-Type": "application/json" } }
  );
  check(registerRes, { "register succeeded": (r) => r.status === 201 });

  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { "Content-Type": "application/json" } }
  );
  check(loginRes, { "login succeeded": (r) => r.status === 200 });
  const token = loginRes.json("accessToken");
  const authHeaders = { headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` } };

  const goalRes = http.post(
    `${BASE_URL}/api/v1/goals`,
    JSON.stringify({ startingWeightKg: 90, targetWeightKg: 80, targetDate: "2027-01-01" }),
    authHeaders
  );
  check(goalRes, { "goal created": (r) => r.status === 201 });

  const weightRes = http.post(`${BASE_URL}/api/v1/weight`, JSON.stringify({ weightKg: 90 }), authHeaders);
  check(weightRes, { "weight logged": (r) => r.status === 201 });

  // A signed WhatsApp webhook turn — the AI-triggering, cost-relevant path
  // (docs/backlog L10-1007's "direct cost driver"). Requires the account
  // to already have a CONNECTED WhatsApp account and an active
  // subscription in the target environment's seed data — this script
  // doesn't create those, since neither has a public self-service API for
  // load-test purposes (WhatsApp requires a real opt-in round trip;
  // subscriptions require a real Stripe checkout). Skip this block, or
  // point WA_PHONE at pre-seeded load-test accounts, if that seed data
  // doesn't exist yet in the target environment.
  const phone = __ENV.WA_PHONE;
  if (phone) {
    const payload = JSON.stringify({
      object: "whatsapp_business_account",
      entry: [{ id: "0", changes: [{ value: {
        messaging_product: "whatsapp",
        metadata: { display_phone_number: phone, phone_number_id: "load-test" },
        messages: [{ from: phone, id: `wamid.k6_${__VU}_${__ITER}_${Date.now()}`, timestamp: `${Math.floor(Date.now() / 1000)}`, text: { body: "How's my progress?" }, type: "text" }],
      } }] }],
    });
    const signature = "sha256=" + hmacSha256Hex(WHATSAPP_APP_SECRET, payload);
    const webhookRes = http.post(`${BASE_URL}/api/v1/whatsapp/webhook`, payload, {
      headers: { "Content-Type": "application/json", "X-Hub-Signature-256": signature },
    });
    check(webhookRes, { "webhook accepted": (r) => r.status === 200 });
  }

  const subRes = http.get(`${BASE_URL}/api/v1/subscriptions/me`, authHeaders);
  check(subRes, { "subscription status readable": (r) => r.status === 200 });

  sleep(1);
}
