#!/usr/bin/env python3
"""Smoke tests for the public course authoring format."""
import copy
import importlib.util
import json
import unittest
from pathlib import Path

MODULE = Path(__file__).with_name("validate-course.py")
spec = importlib.util.spec_from_file_location("course_validator", MODULE)
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)
COURSE = json.loads(Path(__file__).parents[1].joinpath("app/src/main/assets/courses/trigonometry.json").read_text())


class CourseValidatorTests(unittest.TestCase):
    def test_built_in_course(self):
        self.assertEqual(validator.validate(COURSE), (26, 89))

    # @spec spec://modules/learning/PROP-011-course-json-format#concept
    def test_linear_course_with_one_review_per_concept(self):
        lessons = COURSE["lessons"]
        self.assertEqual(len(lessons), 26)
        self.assertEqual([len(lessons[i]["concept"]["blocks"]) for i in (0, 2, 4)], [8, 6, 6])
        for main, review in zip(lessons[::2], lessons[1::2]):
            self.assertTrue(main["concept"]["blocks"])
            self.assertTrue(any("check" in block for block in main["concept"]["blocks"]))
            self.assertTrue(review["id"].startswith("review-"))
            self.assertGreaterEqual(len(review["exercises"]), 2)
            self.assertNotIn("concept", review)
        self.assertNotIn("concept", COURSE["lessons"][1])
        self.assertEqual(validator.validate(COURSE), (26, 89))

    def test_pr3_lessons_and_exercises_are_preserved(self):
        ids = ("triangles-home", "triangles-nature", "opposite-adjacent", "sine", "cosine", "tangent", "which-function", "final-challenge")
        lessons = {lesson["id"]: lesson for lesson in COURSE["lessons"]}
        self.assertTrue(set(ids) <= lessons.keys())
        self.assertEqual(sum(len(lessons[id]["exercises"]) for id in ids), 48)

    def test_invalid_concept_references(self):
        for field, value in (("visual", "unknown_visual"), ("text", {"gn-PY": "", "es": "text"})):
            course = copy.deepcopy(COURSE)
            course["lessons"][0]["concept"]["blocks"][0][field] = value
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, rf"concept\.blocks\[0\]\.{field}"):
                validator.validate(course)
        course = copy.deepcopy(COURSE)
        course["lessons"][0]["concept"]["blocks"][2]["check"]["correctOptionId"] = "missing"
        with self.assertRaisesRegex(ValueError, r"concept\.blocks\[2\]\.check\.correctOptionId"):
            validator.validate(course)

    def test_missing_translation_reports_path(self):
        course = copy.deepcopy(COURSE)
        del course["lessons"][0]["exercises"][0]["prompt"]["es"]
        with self.assertRaisesRegex(ValueError, r"lessons\[0\]\.exercises\[0\]\.prompt\.es"):
            validator.validate(course)

    def test_wrong_answer_reference_rejected(self):
        course = copy.deepcopy(COURSE)
        course["lessons"][0]["exercises"][0]["correctOptionId"] = "missing"
        with self.assertRaisesRegex(ValueError, "correctOptionId"):
            validator.validate(course)

    def test_unsupported_visual_rejected(self):
        course = copy.deepcopy(COURSE)
        course["lessons"][0]["exercises"][0]["visual"] = "obsolete_ui"
        with self.assertRaisesRegex(ValueError, "visual"):
            validator.validate(course)

    def test_triangle_contract_rejected(self):
        for field, value in (("angleDegrees", 90), ("angleDegrees", -1), ("base", "")):
            course = copy.deepcopy(COURSE)
            course["lessons"][2]["exercises"][0]["triangle"][field] = value
            with self.subTest(field=field, value=value), self.assertRaisesRegex(ValueError, "triangle"):
                validator.validate(course)

    def test_lesson_source_required(self):
        for field, value in (("madeWith", None), ("basedOn", "Otro")):
            course = copy.deepcopy(COURSE)
            course["lessons"][1]["source"][field] = value
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, rf"lessons\[1\]\.source\.{field}"):
                validator.validate(course)
        course = copy.deepcopy(COURSE)
        del course["lessons"][1]["source"]
        with self.assertRaisesRegex(ValueError, r"lessons\[1\]\.source"):
            validator.validate(course)

    def test_exercise_without_picture_rejected(self):
        course = copy.deepcopy(COURSE)
        del course["lessons"][2]["exercises"][0]["triangle"]
        with self.assertRaisesRegex(ValueError, "visual part"):
            validator.validate(course)
        course = copy.deepcopy(COURSE)
        course["lessons"][2]["exercises"][2]["triangle"] = course["lessons"][2]["exercises"][0]["triangle"]
        with self.assertRaisesRegex(ValueError, "without a triangle"):
            validator.validate(course)

    def test_matching_has_three_cards_per_column(self):
        course = copy.deepcopy(COURSE)
        exercise = course["lessons"][2]["exercises"][2]
        exercise["leftItems"].pop()
        with self.assertRaisesRegex(ValueError, "three cards"):
            validator.validate(course)

    def test_templates_cover_every_visual(self):
        path = Path(__file__).parents[1].joinpath("app/src/main/assets/templates/exercise-templates.json")
        templates = json.loads(path.read_text())
        validator.validate(templates)
        used = {(e["type"], e["visual"]) for l in templates["lessons"] for e in l["exercises"]}
        self.assertEqual(used, {(t, v) for t, vs in validator.VISUALS.items() for v in vs})


if __name__ == "__main__":
    unittest.main()
