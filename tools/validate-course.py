#!/usr/bin/env python3
"""Validate the built-in localized course package without third-party dependencies."""
import json
import sys
from pathlib import Path

VISUALS = {
    "multiple_choice": {"standard_choice", "triangle_choice", "angle_builder"},
    "input": {"standard_number", "standard_fraction", "fraction_triangle", "straight_angle"},
    "matching": {"standard_pairs", "ratio_pairs", "angle_pairs"},
    "step_by_step": {"standard_steps", "radical_fraction"},
}


def check(condition, path, message):
    if not condition:
        raise ValueError(f"{path}: {message}")


def string(value, path):
    check(isinstance(value, str) and bool(value.strip()), path, "expected nonempty string")


def localized(value, path):
    check(isinstance(value, dict), path, "expected object")
    for language in ("gn-PY", "es"):
        string(value.get(language), f"{path}.{language}")


def answers(value, path):
    check(isinstance(value, list) and bool(value), path, "expected nonempty array")
    for index, answer in enumerate(value):
        string(answer, f"{path}[{index}]")


def unique(items, path):
    ids = []
    for index, item in enumerate(items):
        check(isinstance(item, dict), f"{path}[{index}]", "expected object")
        string(item.get("id"), f"{path}[{index}].id")
        ids.append(item["id"])
    check(len(set(ids)) == len(ids), path, "duplicate id")
    return set(ids)


def options(items, path, diagram=False):
    check(isinstance(items, list) and bool(items), path, "expected nonempty array")
    ids = unique(items, path)
    for index, item in enumerate(items):
        localized(item.get("text"), f"{path}[{index}].text")
        if diagram:
            check(item.get("diagram") in {"acute", "right", "straight"}, f"{path}[{index}].diagram", "unsupported diagram")
    return ids


def validate(data):
    check(isinstance(data, dict), "course", "expected object")
    check(data.get("schemaVersion") == 2, "course.schemaVersion", "expected 2")
    string(data.get("id"), "course.id")
    check(type(data.get("revision")) is int and data["revision"] > 0, "course.revision", "expected positive integer")
    check(data.get("defaultLocale") == "gn-PY", "course.defaultLocale", "expected gn-PY")
    localized(data.get("title"), "course.title")
    lessons = data.get("lessons")
    check(isinstance(lessons, list) and 1 <= len(lessons) <= 10, "course.lessons", "expected 1–10 lessons")
    unique(lessons, "course.lessons")
    all_ids = set()
    for li, lesson in enumerate(lessons):
        path = f"course.lessons[{li}]"
        for field in ("title", "objective"):
            localized(lesson.get(field), f"{path}.{field}")
        check(type(lesson.get("xpReward")) is int and lesson["xpReward"] >= 0, f"{path}.xpReward", "expected nonnegative integer")
        theory = lesson.get("theory")
        check(isinstance(theory, list), f"{path}.theory", "expected array")
        for bi, block in enumerate(theory):
            check(isinstance(block, dict) and block.get("type") in {"text", "formula", "example"}, f"{path}.theory[{bi}].type", "unsupported type")
            localized(block.get("body"), f"{path}.theory[{bi}].body")
        exercises = lesson.get("exercises")
        check(isinstance(exercises, list) and bool(exercises), f"{path}.exercises", "expected nonempty array")
        ids = unique(exercises, f"{path}.exercises")
        check(not all_ids.intersection(ids), f"{path}.exercises", "duplicate course-wide exercise id")
        all_ids.update(ids)
        for ei, exercise in enumerate(exercises):
            ep = f"{path}.exercises[{ei}]"
            kind = exercise.get("type")
            visual = exercise.get("visual")
            check(visual in VISUALS.get(kind, set()), f"{ep}.visual", "unsupported type/visual")
            if "triangle" in exercise:
                triangle = exercise["triangle"]
                check(isinstance(triangle, dict), f"{ep}.triangle", "expected object")
                degrees = triangle.get("angleDegrees")
                check(type(degrees) in (int, float) and 0 < degrees < 90, f"{ep}.triangle.angleDegrees", "angle must be between 0 and 90")
                for field in ("angleLabel", "base", "opposite", "hypotenuse"):
                    string(triangle.get(field), f"{ep}.triangle.{field}")
            for field in ("prompt", "hint"):
                localized(exercise.get(field), f"{ep}.{field}")
            solution = exercise.get("solutionSteps")
            check(isinstance(solution, list) and bool(solution), f"{ep}.solutionSteps", "expected nonempty array")
            for si, step in enumerate(solution):
                localized(step, f"{ep}.solutionSteps[{si}]")
            if kind == "multiple_choice":
                ids = options(exercise.get("options"), f"{ep}.options")
                check(len(ids) >= 2, f"{ep}.options", "at least two options required")
                check(exercise.get("correctOptionId") in ids, f"{ep}.correctOptionId", "unknown option")
                if visual == "angle_builder":
                    try:
                        target = int(exercise["correctOptionId"])
                    except ValueError:
                        target = -1
                    check(0 <= target <= 180, f"{ep}.correctOptionId", "angle must be 0–180")
            elif kind == "input":
                answers(exercise.get("acceptedAnswers"), f"{ep}.acceptedAnswers")
            elif kind == "matching":
                left = options(exercise.get("leftItems"), f"{ep}.leftItems")
                right_items = exercise.get("rightItems")
                right = options(right_items, f"{ep}.rightItems", diagram=visual == "angle_pairs")
                check(len(left) == len(right), ep, "matching columns must have same length")
                pairs = exercise.get("pairs")
                check(isinstance(pairs, list) and len(pairs) == len(left), f"{ep}.pairs", "incomplete pairs")
                relation = {}
                for pi, pair in enumerate(pairs):
                    check(isinstance(pair, dict), f"{ep}.pairs[{pi}]", "expected object")
                    relation[pair.get("leftId")] = pair.get("rightId")
                check(set(relation) == left and set(relation.values()) == right, f"{ep}.pairs", "unknown, repeated or missing pair")
                if visual == "ratio_pairs":
                    for ri, item in enumerate(right_items):
                        for locale in ("gn-PY", "es"):
                            check("/" in item["text"][locale], f"{ep}.rightItems[{ri}].text.{locale}", "expected fraction")
            else:
                steps = exercise.get("steps")
                check(isinstance(steps, list) and bool(steps), f"{ep}.steps", "expected nonempty array")
                for si, step in enumerate(steps):
                    for field in ("prompt", "explanation"):
                        localized(step.get(field), f"{ep}.steps[{si}].{field}")
                    answers(step.get("acceptedAnswers"), f"{ep}.steps[{si}].acceptedAnswers")
    return len(lessons), len(all_ids)


def main():
    path = Path(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/courses/trigonometry.json")
    try:
        count = validate(json.loads(path.read_text(encoding="utf-8")))
    except (OSError, json.JSONDecodeError, ValueError) as error:
        print(f"Invalid course: {error}", file=sys.stderr)
        return 1
    print(f"Valid course v2: {count[0]} lessons, {count[1]} exercises")
    return 0


if __name__ == "__main__":
    sys.exit(main())
