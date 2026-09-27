import test from "node:test";
import assert from "node:assert/strict";
import { TutorJob, outputText } from "./job.js";

test("Tutor answer is short and strips raw math markup", () => {
  const response = text => ({ candidates: [{ content: { parts: [{ text }] } }] });
  assert.equal(outputText(response("$$\\sin(30^\\circ) = \\frac{1}{2}$$")), "sin(30°) = 1/2");
  assert.equal(outputText(response("**Respuesta:** $$\\sin(30^\\circ) = \\frac{\\text{Cateto Opuesto}}{\\text{Hipotenusa}} = \\frac{1}{2}$$")),
    "Respuesta: sin(30°) = Cateto Opuesto/Hipotenusa = 1/2");
  assert.equal(outputText(response("**Result:** 1/2. " + "A very long explanation ".repeat(30))).length <= 240, true);
});

test("same ID survives lost client response without another provider call", async () => {
  let saved, alarm, calls = [];
  const storage = {
    get: async () => saved,
    put: async (_, value) => { saved = structuredClone(value); },
    getAlarm: async () => alarm,
    setAlarm: async value => { alarm = value; },
    deleteAll: async () => { saved = undefined; alarm = undefined; },
  };
  const fetcher = async (url, options) => {
    calls.push(url);
    assert.equal(options.headers["x-goog-api-key"], "test");
    const request = JSON.parse(options.body);
    assert.deepEqual(request.contents, [{ parts: [{ text: "Mba'épa seno?" }] }]);
    assert.equal(request.generationConfig.thinkingConfig.thinkingLevel, "low");
    assert.match(request.systemInstruction.parts[0].text, /no Markdown, LaTeX/);
    assert.match(request.systemInstruction.parts[0].text, /Spanish sentence in Spanish even when it contains one or two Guaraní words/);
    assert.match(request.systemInstruction.parts[0].text, /mostly Guaraní sentence in simple Guaraní/);
    assert.match(request.systemInstruction.parts[0].text, /explicit request for Spanish or Guaraní/);
    return Response.json({ candidates: [{ content: { parts: [{ text: "Peteĩ mbyte." }] } }] });
  };
  const job = new TutorJob(storage, { GEMINI_API_KEY: "test", MODEL: "gemini-3.5-flash-lite" }, fetcher, () => 1000);
  assert.equal((await job.enqueue("Mba'épa seno?")).status, "queued");
  assert.equal((await job.enqueue("Mba'épa seno?")).status, "queued");
  assert.deepEqual(await job.enqueue("Different"), { error: "id_conflict" });
  await job.alarm();
  assert.deepEqual(await job.status(), { status: "completed", answer: "Peteĩ mbyte." });
  assert.equal((await job.enqueue("Mba'épa seno?")).status, "completed");
  assert.equal(calls.filter(url => url.endsWith("/models/gemini-3.5-flash-lite:generateContent")).length, 1);
});

test("concurrent retries with one ID claim budget once and keep the first question", async () => {
  let saved, alarm, claims = 0, allowClaim;
  const claimReady = new Promise(resolve => { allowClaim = resolve; });
  const storage = {
    get: async () => saved,
    put: async (_, value) => { saved = structuredClone(value); },
    getAlarm: async () => alarm,
    setAlarm: async value => { alarm = value; },
  };
  const job = new TutorJob(storage, { REQUESTS: {
    getByName: () => ({ claim: async () => { claims++; await claimReady; return true; } }),
  } });
  const first = job.enqueue("sin 30°?");
  const retry = job.enqueue("sin 30°?");
  const conflict = job.enqueue("cos 30°?");
  allowClaim();
  assert.deepEqual(await Promise.all([first, retry, conflict]), [
    { status: "queued" }, { status: "queued" }, { error: "id_conflict" },
  ]);
  assert.equal(claims, 1);
  assert.equal(saved.question, "sin 30°?");
});

test("ambiguous provider failure is reported without automatic resubmission", async () => {
  let saved, alarm;
  const storage = {
    get: async () => saved,
    put: async (_, value) => { saved = structuredClone(value); },
    getAlarm: async () => alarm,
    setAlarm: async value => { alarm = value; },
    deleteAll: async () => {},
  };
  let calls = 0;
  const job = new TutorJob(storage, { GEMINI_API_KEY: "test", MODEL: "gemini-3.5-flash-lite" }, async () => { calls++; throw Error("lost"); }, () => 1000);
  await job.enqueue("Question");
  await job.alarm();
  assert.deepEqual(await job.status(), { status: "error", error: "provider_connect_failed" });
  await job.alarm();
  assert.equal(calls, 1);
});

test("failed cleanup alarm preserves the answer and retries deletion scheduling", async () => {
  let saved, alarm, now = 1000, alarmFailures = 0, calls = 0;
  const storage = {
    get: async () => saved,
    put: async (_, value) => { saved = structuredClone(value); },
    getAlarm: async () => alarm,
    setAlarm: async value => {
      if (value > now + 1000 && alarmFailures++ === 0) throw Error("temporary storage error");
      alarm = value;
    },
  };
  const job = new TutorJob(storage, { GEMINI_API_KEY: "test", MODEL: "gemini-3.8-flash" },
    async () => { calls++; return Response.json({ candidates: [{ content: { parts: [{ text: "1/2" }] } }] }); },
    () => now);
  await job.enqueue("sin 30°?");
  alarm = undefined; // Cloudflare consumes the due alarm before invoking the handler.
  await assert.rejects(job.alarm(), /temporary storage error/);
  assert.deepEqual(await job.status(), { status: "completed", answer: "1/2" });
  await job.alarm();
  assert.equal(alarm, 1000 + 24 * 60 * 60 * 1000);
  assert.equal(calls, 1);
  now = alarm;
  await job.alarm();
  assert.deepEqual(await job.status(), { status: "expired" });
  assert.equal(saved.question, undefined);
  assert.equal(saved.answer, undefined);
});

test("provider rejection reports safe status without response details", async () => {
  let saved, alarm;
  const storage = {
    get: async () => saved,
    put: async (_, value) => { saved = structuredClone(value); },
    getAlarm: async () => alarm,
    setAlarm: async value => { alarm = value; },
  };
  const job = new TutorJob(storage, { GEMINI_API_KEY: "test", MODEL: "gemini-3.8-flash" },
    async () => Response.json({ error: { status: "PERMISSION_DENIED", message: "private details" } }, { status: 403 }), () => 1000);
  await job.enqueue("Question");
  await job.alarm();
  assert.deepEqual(await job.status(), { status: "error", error: "provider_http_403_PERMISSION_DENIED" });
});

test("monthly limit blocks new IDs but does not consume quota for retries", async () => {
  let saved, alarm, claims = 0, calls = 0;
  const storage = {
    get: async () => saved,
    put: async (_, value) => { saved = structuredClone(value); },
    getAlarm: async () => alarm,
    setAlarm: async value => { alarm = value; },
    deleteAll: async () => {},
  };
  const env = { GEMINI_API_KEY: "test", MODEL: "gemini-3.5-flash-lite", REQUESTS: {
    getByName: () => ({ claim: async () => { claims++; return claims === 1; } }),
  } };
  const job = new TutorJob(storage, env, async () => {
    calls++;
    return Response.json({ candidates: [{ content: { parts: [{ text: "Answer" }] } }] });
  }, () => 1000);
  assert.equal((await job.enqueue("Question")).status, "queued");
  assert.equal((await job.enqueue("Question")).status, "queued");
  assert.equal(claims, 1);
  await job.alarm();
  assert.deepEqual(await job.status(), { status: "completed", answer: "Answer" });
  assert.equal(calls, 1);
  saved = undefined;
  assert.deepEqual(await job.enqueue("Second question"), { error: "monthly_limit" });
  assert.equal(calls, 1);
});

test("expired request retains an ID tombstone and never resubmits", async () => {
  let saved, alarm, now = 1000, calls = 0;
  const storage = {
    get: async () => saved,
    put: async (_, value) => { saved = structuredClone(value); },
    getAlarm: async () => alarm,
    setAlarm: async value => { alarm = value; },
  };
  const job = new TutorJob(storage, { GEMINI_API_KEY: "test", MODEL: "gemini-3.5-flash-lite" }, async () => {
    calls++;
    return Response.json({ candidates: [{ content: { parts: [{ text: "Answer" }] } }] });
  }, () => now);
  await job.enqueue("Question");
  await job.alarm();
  now += 24 * 60 * 60 * 1000;
  await job.alarm();
  assert.deepEqual(await job.status(), { status: "expired" });
  assert.deepEqual(await job.enqueue("Question"), { status: "expired" });
  assert.equal(calls, 1);
  assert.equal(saved.question, undefined);
  assert.equal(saved.answer, undefined);
});
