// @spec spec://modules/android/PROP-010-android-demo-architecture#online
import test from "node:test";
import assert from "node:assert/strict";
import { readJsonBody } from "./body.js";

test("body limit applies without Content-Length", async () => {
  const request = bytes => new Request("https://example.test/requests/id", {
    method: "POST", duplex: "half",
    body: new ReadableStream({ start(controller) { controller.enqueue(new TextEncoder().encode(bytes)); controller.close(); } }),
  });
  assert.deepEqual(await readJsonBody(request('{"question":"2+2"}')), { value: { question: "2+2" } });
  assert.deepEqual(await readJsonBody(request('x'.repeat(4097))), { error: "too_large" });
  assert.deepEqual(await readJsonBody(request('{bad')), { error: "invalid_json" });
});
