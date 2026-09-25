package com.guarani.mathdemo.course

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// @spec spec://modules/android/PROP-010-android-demo-architecture#course-schema
data class Course(
    val id: String,
    val locale: String,
    val title: String,
    val lessons: List<Lesson>,
)

data class Lesson(
    val id: String,
    val title: String,
    val objective: String,
    val theory: List<TheoryBlock>,
    val exercises: List<Exercise>,
    val xpReward: Int,
)

data class TheoryBlock(val type: String, val body: String)

data class Option(val id: String, val text: String)

sealed interface Exercise {
    val id: String
    val prompt: String
    val hint: String
    val solutionSteps: List<String>
}

data class ChoiceExercise(
    override val id: String,
    override val prompt: String,
    override val hint: String,
    override val solutionSteps: List<String>,
    val options: List<Option>,
    val correctOptionId: String,
) : Exercise

data class InputExercise(
    override val id: String,
    override val prompt: String,
    override val hint: String,
    override val solutionSteps: List<String>,
    val acceptedAnswers: List<String>,
) : Exercise

data class MatchingExercise(
    override val id: String,
    override val prompt: String,
    override val hint: String,
    override val solutionSteps: List<String>,
    val leftItems: List<Option>,
    val rightItems: List<Option>,
    val pairs: Map<String, String>,
) : Exercise

data class StepItem(
    val prompt: String,
    val acceptedAnswers: List<String>,
    val explanation: String,
)

data class StepExercise(
    override val id: String,
    override val prompt: String,
    override val hint: String,
    override val solutionSteps: List<String>,
    val steps: List<StepItem>,
) : Exercise

object CourseLoader {
    fun load(context: Context, locale: String = "gn"): Course {
        val asset = if (locale == "es") "courses/trigonometry-es.json" else "courses/trigonometry.json"
        return context.assets.open(asset).bufferedReader().use {
            parse(it.readText())
        }
    }

    fun parse(json: String): Course {
        val root = JSONObject(json)
        require(root.getInt("schemaVersion") == 1) { "Unsupported course schema" }
        val lessons = root.getJSONArray("lessons").objects().map { lesson ->
            Lesson(
                id = lesson.requiredString("id"),
                title = lesson.requiredString("title"),
                objective = lesson.requiredString("objective"),
                theory = lesson.getJSONArray("theory").objects().map { block ->
                    TheoryBlock(block.requiredString("type"), block.requiredString("body"))
                },
                exercises = lesson.getJSONArray("exercises").objects().map(::parseExercise),
                xpReward = lesson.optInt("xpReward", 100),
            )
        }
        require(lessons.isNotEmpty()) { "Course has no lessons" }
        require(lessons.map { it.id }.distinct().size == lessons.size) { "Duplicate lesson ID" }
        val exerciseIds = lessons.flatMap { it.exercises.map(Exercise::id) }
        require(exerciseIds.isNotEmpty() && exerciseIds.distinct().size == exerciseIds.size) {
            "Exercise IDs must be non-empty and unique"
        }
        return Course(
            id = root.requiredString("id"),
            locale = root.requiredString("locale"),
            title = root.requiredString("title"),
            lessons = lessons,
        )
    }

    private fun parseExercise(json: JSONObject): Exercise {
        val id = json.requiredString("id")
        val prompt = json.requiredString("prompt")
        val hint = json.requiredString("hint")
        val solution = json.getJSONArray("solutionSteps").strings()
        require(solution.isNotEmpty()) { "Exercise $id has no solution" }
        return when (json.requiredString("type")) {
            "multiple_choice" -> {
                val options = json.getJSONArray("options").objects().map {
                    Option(it.requiredString("id"), it.requiredString("text"))
                }
                val answer = json.requiredString("correctOptionId")
                require(options.any { it.id == answer }) { "Exercise $id has an unknown correct option" }
                ChoiceExercise(id, prompt, hint, solution, options, answer)
            }
            "input" -> InputExercise(id, prompt, hint, solution, json.getJSONArray("acceptedAnswers").strings())
            "matching" -> {
                val left = json.getJSONArray("leftItems").objects().map {
                    Option(it.requiredString("id"), it.requiredString("text"))
                }
                val right = json.getJSONArray("rightItems").objects().map {
                    Option(it.requiredString("id"), it.requiredString("text"))
                }
                val pairs = json.getJSONArray("pairs").objects().associate {
                    it.requiredString("leftId") to it.requiredString("rightId")
                }
                require(left.isNotEmpty() && pairs.keys == left.map(Option::id).toSet()) {
                    "Exercise $id has incomplete matching pairs"
                }
                require(pairs.values.all { value -> right.any { it.id == value } }) {
                    "Exercise $id has an unknown matching target"
                }
                MatchingExercise(id, prompt, hint, solution, left, right, pairs)
            }
            "step_by_step" -> StepExercise(
                id,
                prompt,
                hint,
                solution,
                json.getJSONArray("steps").objects().map { step ->
                    StepItem(
                        step.requiredString("prompt"),
                        step.getJSONArray("acceptedAnswers").strings(),
                        step.requiredString("explanation"),
                    )
                },
            ).also { require(it.steps.isNotEmpty()) { "Exercise $id has no steps" } }
            else -> error("Unsupported exercise type in $id")
        }
    }

    private fun JSONObject.requiredString(key: String): String = getString(key).also {
        require(it.isNotBlank()) { "Missing $key" }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
}
