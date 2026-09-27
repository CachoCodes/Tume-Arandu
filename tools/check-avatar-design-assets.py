"""Check the shipped avatar layers and collection against the approved design sources."""

import json
from pathlib import Path
from xml.etree import ElementTree

root = Path(__file__).resolve().parents[1]
design = root / "docs/design/profile-avatar/spec"
assets = root / "app/src/main/assets/avatar"

assert json.loads((assets / "unlock-rules.json").read_text()) == json.loads((design / "unlock-rules.json").read_text())

layers = []

def visit(element, when=None):
    name = element.tag.rsplit("}", 1)[-1]
    if name == "sc-if":
        when = element.attrib["value"].strip("{}")
    elif name in {"path", "circle", "ellipse", "rect"}:
        layers.append({"tag": name, "when": when, **element.attrib})
    for child in element:
        visit(child, when)

visit(ElementTree.parse(design / "avatar-template.svg").getroot())
assert layers == json.loads((assets / "avatar-template.json").read_text())
assert len(layers) == 109

rules = json.loads((assets / "unlock-rules.json").read_text())
items = [item for category in rules["categories"] for item in category["items"] if item.get("countsInCollection", True)]
assert len(items) == 22
assert sum(item["unlock"]["kind"] == "free" for item in items) == 11
print("avatar assets match design: 109 layers, 22 collection items, 11 initially unlocked")
