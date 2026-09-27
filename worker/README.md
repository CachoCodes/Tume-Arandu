# Personal Tutor relay

Cloudflare Worker for one trusted user's Online Tutor. The Android app sends a question with a stable UUID; the Worker accepts it once, calls Gemini independently of the phone connection, stores the answer temporarily, and exposes status by the same UUID. Questions and answers are deleted after 24 hours; a content-free ID tombstone remains to prevent late duplicate billing. A retry after a lost phone connection reuses the UUID. A provider submission with an uncertain outcome is reported as an error instead of being repeated automatically. At most 100 new questions are accepted per UTC calendar month through this Worker.

Questions without an explicit mathematics term or expression return `422 {"error":"not_math"}` before a job, monthly budget claim, or model call. The Android Tutor shows a rephrase message and allows another question. This free rule-based filter can reject ambiguous wording or miss a cleverly phrased off-topic question; the model instruction is a secondary guard if one gets through.

The model is instructed to answer in plain text, usually with a result and at most one short explanation. The Worker strips common Markdown/LaTeX markup and limits the visible answer to 240 characters. Request bodies are limited to 4096 actual bytes, including when `Content-Length` is absent. Concurrent retries with the same UUID are serialized before the monthly budget claim. If scheduling the 24-hour cleanup alarm fails, a retried alarm keeps the completed answer and schedules cleanup again.

## Setup

1. In `worker/`, run `npm ci` and `npx wrangler login`.
2. Run `npx wrangler deploy`. It will return a `https://…workers.dev` URL. Until secrets are set, requests return `unconfigured`.
3. `MODEL` is `gemini-3.8-flash` in `wrangler.jsonc`; the Worker calls Google's `generateContent` endpoint. Set `MODEL` to an empty string and deploy to disable AI calls.
4. Run `npx wrangler secret put GEMINI_API_KEY` and paste the key at Wrangler's prompt. Never put it in source files or chat.
5. Generate a random personal access token, for example `openssl rand -hex 32`, and run `npx wrangler secret put TUTOR_ACCESS_TOKEN`, entering that token at Wrangler's prompt.
6. In the Android Tutor screen, select **Configurar** and enter the Worker URL and personal access token. Both are stored privately on this device and excluded from Android backup.

For local checks, use `npm test` and `npx wrangler deploy --dry-run`. No Gemini call is made by these commands. A live smoke test requires both secrets and may incur provider usage. The 100-question Worker cap is a usage guard, not a dollar-denominated hard billing limit. The key owner must set any account-level spending limit and confirm whether the shared key is on a free or paid tier.

The personal token is not suitable for a public APK or shared deployment. A public release needs per-user authentication and abuse limits.
