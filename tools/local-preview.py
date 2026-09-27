from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import threading
import time
from urllib.error import HTTPError
from urllib.request import urlopen

ADB = "/Users/maks/Library/Android/sdk/platform-tools/adb"
DEVICE = "emulator-5554"
WIDTH, HEIGHT = 1080, 2400
GALLERY_PATH = Path(__file__).with_name("exercise-gallery.html")
CONCEPT_LAB_PATH = Path(__file__).with_name("concept-lesson-lab.html")
MULTIPLE_CHOICE_PROTOTYPE = Path(__file__).with_name("multiple-choice-prototype.html")
ANGLE_BUILDER_PROTOTYPE = Path(__file__).with_name("angle-builder-prototype.html")
NUMERIC_INPUT_PROTOTYPE = Path(__file__).with_name("numeric-input-prototype.html")
FRACTION_INPUT_PROTOTYPE = Path(__file__).with_name("fraction-input-prototype.html")
MATCHING_PROTOTYPE = Path(__file__).with_name("matching-prototype.html")
ANGLE_MATCHING_PROTOTYPE = Path(__file__).with_name("angle-matching-prototype.html")
STEP_BY_STEP_PROTOTYPE = Path(__file__).with_name("step-by-step-prototype.html")
GALLERY_IMAGES_DIR = Path(__file__).with_name("exercise-gallery-images")
GALLERY_IMAGE_IDS = (
    "multiple_choice",
    "angle_builder",
    "numeric_input",
    "fraction_input",
    "matching",
    "angle_matching",
    "step_by_step",
)
SCREEN_LOCK = threading.Lock()
SCREEN_CACHE = None
SCREEN_CACHE_AT = 0.0

# @spec spec://modules/learning/FEAT-010-learning-demo#exercises

PAGE = '''<!doctype html>
<html lang="ru"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>GuaraniMath — локальный просмотр</title>
<style>
*{box-sizing:border-box}body{margin:0;background:#101820;color:#e8f0f5;font:14px system-ui,sans-serif;height:100vh;display:grid;grid-template-rows:48px 1fr 36px;overflow:hidden}
header,footer{display:flex;align-items:center;justify-content:space-between;padding:0 16px;background:#18242e;color:#c7d5df}
.gallery-link{padding:7px 11px;border:1px solid #415261;border-radius:8px;color:#e8f0f5;text-decoration:none;font-weight:700}
.gallery-link:hover{background:#243744}
main{display:grid;place-items:center;min-height:0;padding:8px}img{display:block;max-width:96vw;max-height:calc(100vh - 100px);width:auto;height:auto;object-fit:contain;border-radius:8px;box-shadow:0 8px 40px #0008;touch-action:none;user-select:none;-webkit-user-drag:none}
footer{font-size:12px;color:#a9bbc8}
</style>
<header><strong>GuaraniMath · Android-эмулятор</strong><span><a class="gallery-link" href="/tutor">Tutor ↗</a> <a class="gallery-link" href="/concept-lab">Концепты и задания ↗</a> <a class="gallery-link" href="/gallery">Galería de ejercicios ↗</a></span><span>Живой экран</span></header>
<main><img id="screen" src="/screen.png" alt="Текущий экран Android-приложения"></main>
<footer><span>Нажатия и прокрутка передаются в приложение на эмуляторе.</span><span id="status">Подключено к localhost</span></footer>
<script>
const img=document.querySelector('#screen'),status=document.querySelector('#status');let down=null,busy=false,refreshing=true;
function refresh(){if(!busy&&!refreshing){refreshing=true;img.src='/screen.png?t='+Date.now()}}
setInterval(refresh,5000);
img.addEventListener('load',()=>{refreshing=false});
img.addEventListener('pointerdown',e=>{const r=img.getBoundingClientRect();down={x:e.clientX,y:e.clientY,px:(e.clientX-r.left)/r.width*img.naturalWidth,py:(e.clientY-r.top)/r.height*img.naturalHeight};img.setPointerCapture(e.pointerId)});
img.addEventListener('pointerup',async e=>{if(!down)return;const r=img.getBoundingClientRect(),x=(e.clientX-r.left)/r.width*img.naturalWidth,y=(e.clientY-r.top)/r.height*img.naturalHeight;const from=down;down=null;const moved=Math.hypot(e.clientX-from.x,e.clientY-from.y)>12;busy=true;try{await fetch('/input',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(moved?{type:'swipe',x:from.px,y:from.py,x2:x,y2:y}:{type:'tap',x,y})});status.textContent='Экран обновлен';setTimeout(refresh,250)}catch(_){status.textContent='Ошибка связи'}finally{busy=false}});
img.addEventListener('error',()=>{refreshing=false;status.textContent='Ожидание эмулятора'});
</script></html>'''.encode('utf-8')

TUTOR_PAGE = PAGE.replace(
    '<title>GuaraniMath — локальный просмотр</title>'.encode(),
    '<title>GuaraniMath — Tutor</title>'.encode(),
).replace(
    '<strong>GuaraniMath · Android-эмулятор</strong>'.encode(),
    '<strong>GuaraniMath · Tutor из Android-приложения</strong>'.encode(),
).replace(
    '<a class="gallery-link" href="/tutor">Tutor ↗</a>'.encode(),
    '<a class="gallery-link" href="/">Весь экран ↗</a>'.encode(),
)

def screen_png():
    """Only one adb capture may run; overlapping requests reuse the last frame."""
    global SCREEN_CACHE, SCREEN_CACHE_AT
    if SCREEN_CACHE is not None and time.monotonic() - SCREEN_CACHE_AT < 1:
        return SCREEN_CACHE
    if not SCREEN_LOCK.acquire(blocking=False):
        return SCREEN_CACHE
    try:
        try:
            result = subprocess.run([ADB, "-s", DEVICE, "exec-out", "screencap", "-p"], capture_output=True, timeout=8)
            if result.returncode == 0 and result.stdout.startswith(b"\x89PNG\r\n\x1a\n"):
                SCREEN_CACHE = result.stdout
                SCREEN_CACHE_AT = time.monotonic()
        except subprocess.TimeoutExpired:
            pass
        return SCREEN_CACHE
    finally:
        SCREEN_LOCK.release()

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        path = self.path.split("?", 1)[0]
        if path in ("/", "/tutor"):
            body = TUTOR_PAGE if path == "/tutor" else PAGE
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/gallery":
            body = GALLERY_PATH.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/concept-lab":
            body = CONCEPT_LAB_PATH.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/prototype/multiple-choice":
            body = MULTIPLE_CHOICE_PROTOTYPE.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/prototype/angle-builder":
            body = ANGLE_BUILDER_PROTOTYPE.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/prototype/numeric-input":
            body = NUMERIC_INPUT_PROTOTYPE.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/prototype/fraction-input":
            body = FRACTION_INPUT_PROTOTYPE.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/prototype/matching":
            body = MATCHING_PROTOTYPE.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/prototype/angle-matching":
            body = ANGLE_MATCHING_PROTOTYPE.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/prototype/step-by-step":
            body = STEP_BY_STEP_PROTOTYPE.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path.startswith("/gallery-image/"):
            image_name = path[len("/gallery-image/"):]
            image_id = image_name[:-4] if image_name.endswith(".png") else ""
            if image_id not in GALLERY_IMAGE_IDS or path != f"/gallery-image/{image_id}.png":
                self.send_error(404)
                return
            try:
                body = (GALLERY_IMAGES_DIR / f"{image_id}.png").read_bytes()
            except OSError:
                self.send_error(404, "Android screenshot is not available")
                return
            self.send_response(200)
            self.send_header("Content-Type", "image/png")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif path == "/screen.png":
            body = screen_png()
            if body is None:
                self.send_error(503, "Android screenshot is not available")
                return
            self.send_response(200)
            self.send_header("Content-Type", "image/png")
            self.send_header("Cache-Control", "no-store, no-cache, must-revalidate")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        else:
            self.send_error(404)

    def do_POST(self):
        if self.path.split("?", 1)[0] != "/input":
            self.send_error(404)
            return
        try:
            data = json.loads(self.rfile.read(int(self.headers.get("Content-Length", "0"))))
            values = [int(round(float(data[k]))) for k in (("x", "y") if data.get("type") == "tap" else ("x", "y", "x2", "y2"))]
            if any(v < 0 or v > max(WIDTH, HEIGHT) for v in values):
                raise ValueError("coordinates out of range")
            if data.get("type") == "tap":
                args = ["tap", str(values[0]), str(values[1])]
            elif data.get("type") == "swipe":
                args = ["swipe", *(str(v) for v in values), "350"]
            else:
                raise ValueError("unknown input")
            result = subprocess.run([ADB, "-s", DEVICE, "shell", "input", *args], capture_output=True, timeout=8)
            if result.returncode:
                raise RuntimeError(result.stderr.decode(errors="replace"))
            self.send_response(204)
            self.end_headers()
        except Exception as exc:
            self.send_error(400, str(exc))

    def log_message(self, fmt, *args):
        pass


def check_gallery_http():
    """One stdlib-only HTTP/data check; it never calls emulator endpoints."""
    with ThreadingHTTPServer(("127.0.0.1", 0), Handler) as server:
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        base = f"http://127.0.0.1:{server.server_address[1]}"
        try:
            with urlopen(base + "/") as response:
                live_page = response.read().decode("utf-8")
                assert response.status == 200
            assert 'href="/gallery"' in live_page
            assert 'href="/concept-lab"' in live_page
            assert 'href="/tutor"' in live_page

            with urlopen(base + "/tutor") as response:
                tutor_page = response.read().decode("utf-8")
                assert response.status == 200
            assert "Tutor из Android-приложения" in tutor_page
            assert 'src="/screen.png"' in tutor_page

            with urlopen(base + "/concept-lab") as response:
                lab = response.read().decode("utf-8")
                assert response.status == 200
                assert response.headers.get_content_type() == "text/html"
            assert all(f'data-section="{name}"' in lab for name in ("concepts", "tasks"))
            assert all(name in lab for name in ("Прямоугольный треугольник", "Треугольники в жизни", "Измерение с треугольником", "Найди ошибку", "Поставь по порядку", "Отметь все верные", "Сравни и объясни"))

            with urlopen(base + "/gallery") as response:
                gallery = response.read().decode("utf-8")
                assert response.status == 200
                assert response.headers.get_content_type() == "text/html"
            expected = {
                "multiple_choice": "ratios-cosine",
                "angle_builder": "angles-choice",
                "numeric_input": "angles-input",
                "fraction_input": "ratios-sine",
                "matching": "ratios-match",
                "angle_matching": "angles-match",
                "step_by_step": "notable-rationalization",
            }
            assert 'lang="es"' in gallery
            assert 'href="/"' in gallery
            assert "/input" not in gallery
            assert "<form" not in gallery and "__COURSE_JSON__" not in gallery
            assert all(f'id:"{key}",exerciseId:"{exercise_id}"' in gallery for key, exercise_id in expected.items())
            fingerprints = set()
            for image_id in expected:
                with urlopen(base + f"/gallery-image/{image_id}.png") as response:
                    assert response.status == 200
                    assert response.headers.get_content_type() == "image/png"
                    image = response.read()
                    assert image.startswith(b"\x89PNG\r\n\x1a\n")
                    assert len(image) > 50_000, f"{image_id} is too small to be a captured exercise screen"
                    fingerprints.add(hashlib.sha256(image).digest())
            assert len(fingerprints) == len(expected), "each exercise type must have its own screenshot"

            try:
                urlopen(base + "/gallery-image/unknown.png")
            except HTTPError as error:
                assert error.code == 404
            else:
                raise AssertionError("unknown image route must return HTTP 404")
        finally:
            server.shutdown()
            thread.join()
    print("OK: /, /gallery, seven APK screenshot routes, and unknown image 404")


def check_screen_capture():
    from types import SimpleNamespace
    from unittest.mock import patch
    started, release = threading.Event(), threading.Event()
    calls = []
    png = b"\x89PNG\r\n\x1a\nframe"

    def capture(*_args, **_kwargs):
        calls.append(1)
        started.set()
        assert release.wait(1)
        return SimpleNamespace(returncode=0, stdout=png)

    with patch.object(subprocess, "run", capture):
        worker = threading.Thread(target=screen_png)
        worker.start()
        assert started.wait(1)
        assert screen_png() is None and len(calls) == 1
        release.set()
        worker.join(1)
        assert screen_png() == png and len(calls) == 1
    print("OK: simultaneous screenshot requests launch one adb capture")


if __name__ == "__main__":
    if sys.argv[1:] == ["--check"]:
        check_gallery_http()
        check_screen_capture()
    elif sys.argv[1:]:
        raise SystemExit("Usage: python3 tools/local-preview.py [--check]")
    else:
        ThreadingHTTPServer(("127.0.0.1", int(os.environ.get("GUARANI_PREVIEW_PORT", "8765"))), Handler).serve_forever()
