import { DurableObject } from "cloudflare:workers";
import { TutorJob } from "./job.js";
import { isMathQuestion } from "./math.js";
import { readJsonBody } from "./body.js";

const reply = (body, status = 200) => Response.json(body, {
  status,
  headers: { "Cache-Control": "no-store" },
});
const TEXT_MODELS = new Set(["gemini-3.5-flash-lite", "gemini-3.8-flash"]);

// @spec spec://modules/android/PROP-010-android-demo-architecture#online
export default {
  async fetch(request, env) {
    if (!env.TUTOR_ACCESS_TOKEN) {
      return reply({ error: "unconfigured" }, 503);
    }
    if (request.headers.get("Authorization") !== `Bearer ${env.TUTOR_ACCESS_TOKEN}`) {
      return reply({ error: "unauthorized" }, 401);
    }
    const match = /^\/requests\/([0-9a-f-]{36})$/.exec(new URL(request.url).pathname);
    if (!match) return reply({ error: "not_found" }, 404);
    if (request.method !== "POST" && request.method !== "GET") return reply({ error: "method" }, 405);

    if (request.method === "GET") return reply(await env.REQUESTS.getByName(match[1]).status());

    const parsed = await readJsonBody(request);
    if (parsed.error) return reply({ error: parsed.error }, parsed.error === "too_large" ? 413 : 400);
    const body = parsed.value;
    const question = body?.question;
    if (typeof question !== "string" || !question.trim() || question.length > 1000) {
      return reply({ error: "invalid_question" }, 400);
    }
    if (!isMathQuestion(question)) return reply({ error: "not_math" }, 422);
    if (!env.GEMINI_API_KEY || !TEXT_MODELS.has(env.MODEL)) return reply({ error: "unconfigured" }, 503);
    const job = env.REQUESTS.getByName(match[1]);
    const result = await job.enqueue(question.trim());
    return reply(result, result.error === "id_conflict" ? 409 : result.error === "monthly_limit" ? 429 : 202);
  },
};

// @spec spec://modules/android/PROP-010-android-demo-architecture#online
export class TutorRequest extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.job = new TutorJob(ctx.storage, env);
  }
  enqueue(question) { return this.job.enqueue(question); }
  status() { return this.job.status(); }
  alarm() { return this.job.alarm(); }

  async claim() {
    const month = new Date().toISOString().slice(0, 7);
    return this.ctx.storage.transaction(async storage => {
      const budget = await storage.get("budget");
      const count = budget?.month === month ? budget.count : 0;
      if (count >= 100) return false;
      await storage.put("budget", { month, count: count + 1 });
      return true;
    });
  }
}
