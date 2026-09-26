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
        self.assertEqual(validator.validate(COURSE), (7, 21))

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
        course["lessons"][3]["exercises"][2]["triangle"] = course["lessons"][2]["exercises"][0]["triangle"]
        with self.assertRaisesRegex(ValueError, "without a triangle"):
            validator.validate(course)

    def test_templates_cover_every_visual(self):
        path = Path(__file__).parents[1].joinpath("app/src/main/assets/templates/exercise-templates.json")
        templates = json.loads(path.read_text())
        validator.validate(templates)
        used = {(e["type"], e["visual"]) for l in templates["lessons"] for e in l["exercises"]}
        self.assertEqual(used, {(t, v) for t, vs in validator.VISUALS.items() for v in vs})


if __name__ == "__main__":
    unittest.main()
