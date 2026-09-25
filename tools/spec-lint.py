#!/usr/bin/env python3
"""Structural checks for Spec Drive. Python 3.9+, no dependencies."""
import argparse
import re
from pathlib import Path

SPEC_ID = re.compile(r"^(PROP|FEAT|INFRA)-\d+(?:\.[A-Z]+)?(?=-|\.md$)")
WI_ID = re.compile(r"^WI-\d+(?=-|\.md$)")
ADDRESS = re.compile(r"spec://([\w./-]+)#([\w.-]+)")
ANCHOR = re.compile(r"\{#([\w.-]+)\}")
LINK = re.compile(r"\[([^\]]+)\]\(([^)]+)\)")
SKIP = {".git", ".spec-drive", ".prist", "node_modules", "vendor", ".venv", "venv",
        "dist", "build", "coverage", "__pycache__", ".next", ".cache"}
CODE = {".py", ".js", ".jsx", ".ts", ".tsx", ".mjs", ".cjs", ".go", ".rs", ".java",
        ".kt", ".swift", ".cs", ".php", ".rb", ".sql", ".sh", ".vue", ".svelte", ".css", ".scss"}


def walk(root):
    if not root.is_dir() or root.is_symlink():
        return
    for path in sorted(root.iterdir()):
        if path.is_symlink() or path.name in SKIP:
            continue
        if path.is_dir():
            yield from walk(path)
        elif path.is_file():
            yield path


def lint(root):
    root = root.resolve()
    errors, warnings = [], []
    specs, ids, documents = {}, {}, {}

    def read(path):
        if path not in documents:
            documents[path] = path.read_text(encoding="utf-8-sig")
        return documents[path]

    def report(path, message, warning=False):
        (warnings if warning else errors).append(f"{path.relative_to(root)}: {message}")

    for relative in ("specs/SPEC-MAP.md", "specs/BOARD.md", "specs/WAL.md", "specs/common/main.md", "specs/common/structure.md"):
        path = root / relative
        if not path.is_file() or path.is_symlink():
            report(path, "missing file or symbolic link")

    for folder in (root / "specs/common", root / "specs/modules"):
        for path in walk(folder):
            if path.suffix != ".md" or not SPEC_ID.match(path.name):
                continue
            text = read(path)
            identity = SPEC_ID.match(path.name)[0]
            if identity in ids:
                report(path, f"duplicate spec ID {identity}: {ids[identity].relative_to(root)}")
            ids[identity] = path
            anchors = ANCHOR.findall(text)
            if "root" not in anchors:
                report(path, "missing explicit {#root}")
            if len(anchors) != len(set(anchors)):
                report(path, "duplicate anchor")
            front = re.match(r"\A---\r?\n(.*?)\r?\n---(?:\r?\n|$)", text, re.S)
            state = re.search(r"^status:\s*['\"]?([\w-]+)['\"]?\s*$", front[1], re.M) if front else None
            status = state[1] if state else "active"
            if not state:
                report(path, "legacy spec without status; treated as active", True)
            if status not in {"draft", "active", "superseded", "retired"}:
                report(path, f"invalid status {status}")
            address = path.relative_to(root / "specs").with_suffix("").as_posix()
            specs[address] = (path, set(anchors), status, bool(state))

    def check_refs(path, text, code=False):
        for match in ADDRESS.finditer(text):
            address, anchor = match.groups()
            target = specs.get(address)
            if not target:
                report(path, f"missing spec: {match[0]}")
            elif anchor not in target[1]:
                report(path, f"missing anchor: {match[0]}")
            elif code and target[2] != "active":
                report(path, f"@spec must reference active canon: {match[0]} is {target[2]}")
        # Bare or truncated addresses are not silently accepted.
        for token in re.findall(r"spec://[^\s`<>\]\)\"']+", text):
            if not ADDRESS.fullmatch(token):
                report(path, f"invalid spec address (full anchor required): {token}")

    map_path = root / "specs/SPEC-MAP.md"
    map_text = read(map_path) if map_path.is_file() else ""
    map_refs = {m[0] for m in ADDRESS.findall(map_text)}
    for address, (path, anchors, status, explicit) in specs.items():
        check_refs(path, read(path))
        if address not in map_refs:
            report(path, "not registered in SPEC-MAP.md", warning=not explicit)
        for row in map_text.splitlines():
            if f"spec://{address}#" in row and status not in [c.strip(" `") for c in row.split("|")]:
                report(map_path, f"map status disagrees with {address}: expected {status}")
        if status == "superseded":
            section = re.search(r"^## Superseded by[^\n]*\n(.*?)(?=^## |\Z)", read(path), re.M | re.S)
            refs = ADDRESS.findall(section[1]) if section else []
            if not refs:
                report(path, "superseded spec needs Superseded by links")
            for replacement, _ in refs:
                if replacement in specs:
                    other = read(specs[replacement][0])
                    reverse = re.search(r"^## Supersedes[^\n]*\n(.*?)(?=^## |\Z)", other, re.M | re.S)
                    if not reverse or address not in {a for a, _ in ADDRESS.findall(reverse[1])}:
                        report(path, f"missing reciprocal Supersedes in {replacement}")

    work = {}
    for path in walk(root / "specs/work"):
        if path.suffix != ".md" or not WI_ID.match(path.name):
            continue
        identity = WI_ID.match(path.name)[0]
        if identity in work:
            report(path, f"duplicate work ID {identity}")
        work[identity] = path
        text = read(path)
        for section in ("Outcome", "Scope", "Specs", "Acceptance", "Dependencies", "Risks", "Result"):
            if not re.search(rf"^## {section}\s*$", text, re.M):
                report(path, f"missing {section} section")
        if not re.search(r"^Canon action: (none|direct-edit|new-spec|supersede)\s*$", text, re.M):
            report(path, "missing or invalid Canon action")
        criteria = re.findall(r"^- (AC-\d+):\s*\S.*$", text, re.M)
        if not criteria or len(criteria) != len(set(criteria)):
            report(path, "Acceptance needs unique nonempty AC-N criteria")
        check_refs(path, text)

    board = root / "specs/BOARD.md"
    states, section = {}, ""
    for line in read(board).splitlines() if board.is_file() else []:
        if line.startswith("## "):
            section = line[3:].strip()
        if not line.startswith("|"):
            continue
        for label, link in LINK.findall(line):
            match = re.search(r"\bWI-\d+\b", label)
            if not match:
                continue
            identity = match[0]
            if identity in states:
                report(board, f"duplicate BOARD ID {identity}")
            states[identity] = section
            target = (board.parent / link).resolve()
            if target != work.get(identity):
                report(board, f"missing or mismatched WI link: {identity} -> {link}")
                continue
            cells = [c.strip() for c in line.strip("|").split("|")]
            if section not in {"Backlog", "In Progress", "Blocked", "Done"}:
                report(board, f"invalid section for {identity}: {section}")
            if section in {"In Progress", "Done"} and (len(cells) < 2 or not cells[1].startswith("@")):
                report(board, f"{identity} needs owner")
            if section == "Blocked" and (len(cells) < 4 or cells[3] in {"", "—", "-"}):
                report(board, f"{identity} needs blocker")
            if section == "Done":
                text = read(target)
                if "archive" not in target.relative_to(root / "specs/work").parts:
                    report(target, "Done WI must be in work/archive/YYYY/")
                result = text.split("\n## Result", 1)[-1] if "\n## Result" in text else ""
                for criterion in re.findall(r"^- (AC-\d+):", text, re.M):
                    rows = [r for r in result.splitlines() if re.match(rf"\|\s*{criterion}\s*\|", r)]
                    cells = [c.strip() for c in rows[0].strip("|").split("|")] if len(rows) == 1 else []
                    if len(cells) != 3 or cells[1] in {"", "—", "-", "TODO"} or cells[2] != "passed":
                        report(target, f"Done needs named passed evidence for {criterion}")
        if re.search(r"\b(?:FEAT|INFRA)-\d+", line):
            report(board, "legacy spec-as-work row: migrate when resuming this work", True)
    for identity, path in work.items():
        if identity not in states:
            report(path, "WI missing from BOARD")

    wal = root / "specs/WAL.md"
    wal_text = read(wal) if wal.is_file() else ""
    checkpoints = re.findall(r"^### (WI-\d+)[^\n]*\n(.*?)(?=^#{2,3} |\Z)", wal_text, re.M | re.S)
    seen = set()
    for identity, text in checkpoints:
        if identity in seen:
            report(wal, f"duplicate checkpoint {identity}")
        seen.add(identity)
        if states.get(identity) not in {"In Progress", "Blocked"}:
            report(wal, f"checkpoint {identity} has no active BOARD work")
        for field in ("Work", "Updated", "Checkpoint", "Next", "Blocker"):
            if not re.search(rf"^- {field}:\s*\S", text, re.M):
                report(wal, f"{identity}: missing {field}")
        links = LINK.findall(text)
        if not any((wal.parent / link).resolve() == work.get(identity) for _, link in links):
            report(wal, f"{identity}: missing or mismatched Work link")
    for path in (map_path, root / "specs/common/structure.md", root / "specs/common/main.md", wal, root / "specs/TECHDEBT.md"):
        if path.is_file():
            check_refs(path, read(path))
    for path in walk(root):
        if path.suffix not in CODE or "specs" in path.relative_to(root).parts:
            continue
        text = read(path)
        for line in text.splitlines():
            if re.match(r"^\s*(?://|/\*|\*|#|--|<!--).*@spec\b", line):
                marker = line.split("@spec", 1)[1].strip().split()
                if not marker or not ADDRESS.fullmatch(marker[0]):
                    report(path, "@spec requires one complete spec://...#anchor")
                else:
                    check_refs(path, marker[0], code=True)
    return errors, warnings


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", nargs="?", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    try:
        errors, warnings = lint(args.root)
    except (OSError, UnicodeError) as error:
        parser.exit(1, f"spec-lint: {error}\n")
    for message in warnings:
        print(f"WARN {message}")
    for message in errors:
        print(f"ERROR {message}")
    print(f"spec-lint: {len(errors)} errors, {len(warnings)} warnings (structure only)")
    return bool(errors)


if __name__ == "__main__":
    raise SystemExit(main())
