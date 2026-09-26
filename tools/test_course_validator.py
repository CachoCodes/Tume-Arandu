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
COURSE = json.loads(Path(__file__).parents[1].joinpath("app/src/main/assets/courses/trigonometry.json").read_text(encoding="utf-8"))


class CourseValidatorTests(unittest.TestCase):
    def test_built_in_course(self):
        self.assertEqual(validator.validate(COURSE), (8, 48))

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



if __name__ == "__main__":
    unittest.main()
