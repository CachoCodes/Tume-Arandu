import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { extname, resolve, sep } from "node:path";
import { chromium } from "playwright-core";

const root = resolve(fileURLToPath(new URL(".", import.meta.url)));
const outputArg = process.argv.find((arg) => arg.startsWith("--output="))?.slice("--output=".length);
const output = resolve(root, outputArg ?? "../../app/src/main/res/drawable-nodpi/sin30_sculpture.png");
const mime = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
};

const server = createServer(async (request, response) => {
  try {
    const url = new URL(request.url ?? "/", "http://127.0.0.1");
    const relative = decodeURIComponent(url.pathname === "/" ? "/index.html" : url.pathname);
    const file = resolve(root, `.${relative}`);
    if (!file.startsWith(`${root}${sep}`)) throw new Error("Path outside math-prop tool");
    const content = await readFile(file);
    response.writeHead(200, { "Content-Type": mime[extname(file)] ?? "application/octet-stream" });
    response.end(content);
  } catch (error) {
    if (request.url !== "/favicon.ico") console.error(`Asset request failed: ${request.url} (${error.message})`);
    if (!response.headersSent) response.writeHead(404);
    response.end("Not found");
  }
});

const browserPath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const browser = await chromium.launch({
  executablePath: browserPath,
  headless: true,
  args: ["--enable-webgl", "--ignore-gpu-blocklist", "--disable-dev-shm-usage"],
});

try {
  await new Promise((resolveListen) => server.listen(0, "127.0.0.1", resolveListen));
  const address = server.address();
  const page = await browser.newPage({ viewport: { width: 768, height: 768 }, deviceScaleFactor: 1 });
  page.on("pageerror", (error) => console.error(error));
  page.on("console", (message) => {
    if (message.type() === "error") console.error(message.text());
  });
  await page.goto(`http://127.0.0.1:${address.port}/`, { waitUntil: "networkidle" });
  await page.waitForFunction(() => window.mathPropReady === true, { timeout: 30_000 });
  await page.screenshot({ path: output, omitBackground: true });
  console.log(`Rendered 768x768 RGBA PNG: ${output}`);
  await page.close();
} finally {
  await browser.close();
  server.close();
}
