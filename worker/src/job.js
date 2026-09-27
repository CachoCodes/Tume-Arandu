const DAY = 24 * 60 * 60 * 1000;
const SYSTEM_INSTRUCTION = "You are a calm mathematics tutor. Give the direct result first. For a simple fact or calculation, use one short sentence, for example: sin 30° = 1/2. For a harder question, add at most one short sentence explaining the essential step. Never exceed two sentences. Choose the reply language from the student's explicit request for Spanish or Guaraní; otherwise use the dominant language of the whole question, especially its grammar. Answer a Spanish sentence in Spanish even when it contains one or two Guaraní words. Answer a mostly Guaraní sentence in simple Guaraní even when it contains Spanish math terms. An isolated borrowed word, proper name, or shared math term does not change the language. For a language-neutral formula with no language request, default to Spanish. Keep the entire answer in the chosen language. Use plain text only: no Markdown, LaTeX, dollar signs, backslashes, lists, headings, emoji, or extra examples. Write fractions as 1/2. Answer only mathematics; if off topic, say so briefly. Do not grade exercises or add filler.";

// ponytail: this covers common model markup; a richer renderer is needed for complex notation.
export function outputText(response) {
  const text = (response.candidates?.[0]?.content?.parts || [])
    .filter(part => !part.thought && typeof part.text === "string")
    .map(part => part.text)
    .join("\n")
    .replace(/\\(?:text|mathrm)\{([^{}]*)\}/g, "$1")
    .replace(/\\frac\{([^{}]*)\}\{([^{}]*)\}/g, "$1/$2")
    .replace(/\\sqrt\{([^{}]*)\}/g, "√($1)")
    .replace(/\^?\\circ/g, "°")
    .replace(/\\(sin|cos|tan)/g, "$1")
    .replace(/\\[A-Za-z]+/g, "")
    .replace(/^\s*\d+\.\s*/gm, "")
    .replace(/[$*`#{}\\]/g, "")
    .replace(/\s+/g, " ").trim();
  const twoSentences = text.split(/(?<=[.!?])\s+(?=\p{L}|\p{N})/u).slice(0, 2).join(" ");
  if (twoSentences.length <= 240) return twoSentences;
  const cut = twoSentences.slice(0, 240);
  return cut.slice(0, cut.lastIndexOf(" ")).trimEnd() + ".";
}

// @spec spec://modules/android/PROP-010-android-demo-architecture#online
export class TutorJob {
  constructor(storage, env, fetcher = (url, options) => fetch(url, options), now = Date.now) {
    this.storage = storage;
    this.env = env;
    this.fetcher = fetcher;
    this.now = now;
  }

  async enqueue(question) {
    // Serialize retries for this ID while the budget RPC is in flight.
    const previous = this.enqueueTail;
    let release;
    const tail = new Promise(resolve => { release = resolve; });
    this.enqueueTail = tail;
    if (previous) await previous;
    try {
      let job = await this.storage.get("job");
      if (job) {
        if (job.status === "expired") return this.view(job);
        if (job.question !== question) return { error: "id_conflict" };
        if (job.status === "queued" && !await this.storage.getAlarm()) {
          await this.storage.setAlarm(this.now() + 1000);
        }
        return this.view(job);
      }
      const budget = this.env.REQUESTS?.getByName("__monthly_budget");
      if (budget && !await budget.claim()) return { error: "monthly_limit" };
      job = { question, status: "queued", createdAt: this.now(), answer: null };
      await this.storage.put("job", job);
      await this.storage.setAlarm(this.now() + 1000);
      return this.view(job);
    } finally {
      release();
      if (this.enqueueTail === tail) this.enqueueTail = null;
    }
  }

  view(job) {
    if (!job) return { status: "not_found" };
    return { status: job.status, ...(job.answer ? { answer: job.answer } : {}),
      ...(job.error ? { error: job.error } : {}) };
  }

  async status() { return this.view(await this.storage.get("job")); }

  async finish(job) {
    await this.storage.put("job", job);
    await this.storage.setAlarm(job.createdAt + DAY);
  }

  async alarm() {
    const job = await this.storage.get("job");
    if (!job) return;
    if (this.now() >= job.createdAt + DAY) {
      // Keep only the ID's tombstone so a late phone retry cannot bill twice.
      await this.storage.put("job", { status: "expired", createdAt: job.createdAt });
      return;
    }
    if (job.status === "submitting") {
      job.status = "error";
      job.error = "provider_unavailable";
      await this.finish(job);
      return;
    }
    if (job.status === "completed" || job.status === "error") {
      if (!await this.storage.getAlarm()) await this.storage.setAlarm(job.createdAt + DAY);
      return;
    }
    if (job.status === "queued") {
      // A lost provider response is ambiguous. Never resubmit it automatically.
      job.status = "submitting";
      await this.storage.put("job", job);
      let phase = "connect";
      try {
        const response = await this.fetcher(`https://generativelanguage.googleapis.com/v1beta/models/${this.env.MODEL}:generateContent`, {
          method: "POST",
          headers: { "x-goog-api-key": this.env.GEMINI_API_KEY, "Content-Type": "application/json" },
          body: JSON.stringify({
            contents: [{ parts: [{ text: job.question }] }],
            systemInstruction: { parts: [{ text: SYSTEM_INSTRUCTION }] },
            generationConfig: { maxOutputTokens: 1024, thinkingConfig: { thinkingLevel: "low" } },
          }),
        });
        phase = "response";
        if (!response.ok) {
          const data = await response.json().catch(() => ({}));
          const reason = /^[A-Z_]+$/.test(data?.error?.status) ? `_${data.error.status}` : "";
          throw new Error(`provider_http_${response.status}${reason}`);
        }
        const data = await response.json();
        phase = "result";
        const answer = outputText(data);
        job.status = answer ? "completed" : "error";
        job.answer = answer || null;
        job.error = answer ? null : "empty_answer";
      } catch (error) {
        job.status = "error";
        job.error = /^provider_http_\d{3}(?:_[A-Z_]+)?$/.test(error?.message)
          ? error.message : `provider_${phase}_failed`;
      }
      await this.finish(job);
      return;
    }
  }
}
