// @spec spec://modules/android/PROP-010-android-demo-architecture#online
export async function readJsonBody(request, maxBytes = 4096) {
  if (Number(request.headers.get("Content-Length")) > maxBytes) return { error: "too_large" };
  const reader = request.body?.getReader();
  if (!reader) return { error: "invalid_json" };
  const chunks = [];
  let size = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      size += value.byteLength;
      if (size > maxBytes) {
        await reader.cancel().catch(() => {});
        return { error: "too_large" };
      }
      chunks.push(value);
    }
    const bytes = new Uint8Array(size);
    let offset = 0;
    for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
    return { value: JSON.parse(new TextDecoder("utf-8", { fatal: true }).decode(bytes)) };
  } catch {
    return { error: "invalid_json" };
  }
}
