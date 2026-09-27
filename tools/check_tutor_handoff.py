"""Smoke-check the last hint step and Tutor handoff on an installed Android APK.

Uses MainActivity's separate preview progress, then restores the normal map.
"""

import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path


ADB = str(Path(os.environ.get("ANDROID_HOME", Path.home() / "Library/Android/sdk")) / "platform-tools/adb")
ACTIVITY = "com.guarani.mathdemo/.MainActivity"


def adb(*args):
    return subprocess.check_output([ADB, *args], stderr=subprocess.DEVNULL)


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/tutor-handoff.xml")
    root = ET.fromstring(adb("exec-out", "cat", "/sdcard/tutor-handoff.xml"))
    return [node.attrib for node in root.iter("node")]


def find(label):
    for _ in range(5):
        for node in nodes():
            if label in node.get("text", "") or label in node.get("content-desc", ""):
                return node
        time.sleep(0.3)
    raise AssertionError(f"Missing UI label: {label}")


def tap(label):
    node = find(label)
    left, top, right, bottom = map(int, re.findall(r"\d+", node["bounds"]))
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))


def check():
    adb("shell", "am", "force-stop", "com.guarani.mathdemo")
    adb("shell", "am", "start", "-n", ACTIVITY,
        "-e", "preview", "courses/trigonometry.json", "-e", "exercise", "rt-hypotenuse")
    find("Tume Arandu aty ndive ojejapo")
    tap("Ahecha pista")
    find("Pehẽ 1 / 2")
    assert not any("Eporandu AI mbo" in node.get("text", "") for node in nodes())
    tap("Esegi")
    find("Eporandu AI mbo'ehárape")
    tap("Eporandu AI mbo'ehárape")
    find("Eipytyvõ chéve aikũmby")
    tap("Ejevy mbo'epy-pe")
    find("Ahecha pista")  # The solution was not marked complete.
    print("PASS: Tume Arandu credit, final hint → Tutor draft, return without completing exercise")


if __name__ == "__main__":
    try:
        check()
    finally:
        adb("shell", "am", "force-stop", "com.guarani.mathdemo")
        adb("shell", "am", "start", "-n", ACTIVITY)
