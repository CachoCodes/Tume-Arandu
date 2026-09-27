package com.guarani.mathdemo.course

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// @spec spec://modules/learning/PROP-011-course-json-format#validation
const val LESSON_MADE_WITH = "Dulce Duro"
const val LESSON_BASED_ON = "MEC Paraguay"

object CourseLoader {
    private val conceptVisuals = setOf("triangle_vertices", "right_angle", "compare_figures", "catheti", "hypotenuse", "rotated_180", "labeled_sides", "ramp", "ramp_right", "ramp_hypotenuse", "known_sides", "unknown_side", "other_numbers", "angle_arc", "similar_triangles", "sine_ratio", "cosine_ratio", "tangent_ratio", "ratio_choice", "tree_shadow")
    private val visuals = mapOf(
        "multiple_choice" to setOf("triangle_choice", "angle_builder", "standard_choice"),
        "input" to setOf("fraction_triangle", "straight_angle", "standard_number", "standard_fraction"),
        "matching" to setOf("ratio_pairs", "angle_pairs", "standard_pairs"),
        "step_by_step" to setOf("radical_fraction", "standard_steps"),
    )

    const val COURSE_ASSET = "courses/trigonometry.json"
    // Every approved exercise type with template content; opened in preview mode.
    const val TEMPLATES_ASSET = "templates/exercise-templates.json"

    fun load(context: Context, locale: String = "gn", asset: String = COURSE_ASSET): Course =
        context.assets.open(asset).bufferedReader().use { parse(it.readText(), locale) }

    fun parse(json: String, locale: String = "gn"): Course {
        val language = if (locale == "es") "es" else "gn-PY"
        return at("course") {
            val root = JSONObject(json)
            require(root.getInt("schemaVersion") == 2) { "unsupported schemaVersion" }
            require(root.requiredString("defaultLocale") == "gn-PY") { "defaultLocale must be gn-PY" }
            require(root.getInt("revision") > 0) { "revision must be positive" }
            val lessons = root.getJSONArray("lessons").objects().mapIndexed { index, lesson ->
                at("lessons[$index]") {
                    val theory = lesson.getJSONArray("theory").objects().mapIndexed { blockIndex, block ->
                        at("theory[$blockIndex]") {
                            val type = block.requiredString("type")
                            require(type in setOf("text", "formula", "example")) { "unsupported theory type $type" }
                            TheoryBlock(type, block.localized("body", language))
                        }
                    }
                    val exercises = lesson.getJSONArray("exercises").objects().mapIndexed { exerciseIndex, exercise ->
                        at("exercises[$exerciseIndex]") { parseExercise(exercise, language) }
                    }
                    require(exercises.isNotEmpty()) { "exercises must not be empty" }
                    // @spec spec://modules/learning/PROP-011-course-json-format#lesson-source
                    val source = at("source") { lesson.getJSONObject("source") }
                    require(source.requiredString("madeWith") == LESSON_MADE_WITH) { "source.madeWith must be $LESSON_MADE_WITH" }
                    require(source.requiredString("basedOn") == LESSON_BASED_ON) { "source.basedOn must be $LESSON_BASED_ON" }
                    val xp = lesson.getInt("xpReward")
                    require(xp >= 0) { "xpReward must not be negative" }
                    // @spec spec://modules/learning/PROP-011-course-json-format#concept
                    val concept = if (lesson.has("concept")) at("concept") {
                        val blocks = lesson.getJSONObject("concept").getJSONArray("blocks").objects().mapIndexed { blockIndex, block ->
                            at("blocks[$blockIndex]") {
                                val visual = block.requiredString("visual")
                                require(visual in conceptVisuals) { "unsupported visual $visual" }
                                val check = if (block.has("check")) at("check") {
                                    val data = block.getJSONObject("check")
                                    val options = data.getJSONArray("options").objects().map { option -> Option(option.requiredString("id"), option.localized("text", language)) }
                                    require(options.size >= 2 && options.map { it.id }.distinct().size == options.size) { "options must have unique IDs" }
                                    val answer = data.requiredString("correctOptionId")
                                    require(options.any { it.id == answer }) { "correctOptionId does not exist" }
                                    val mode = data.optString("mode")
                                    require(mode.isEmpty() || (mode == "figure" && visual == "compare_figures")) { "invalid check mode" }
                                    ConceptCheck(options, answer, data.localized("good", language), data.localized("bad", language), mode == "figure")
                                } else null
                                ConceptBlock(block.requiredString("id"), block.localized("title", language), block.localized("text", language), visual, check)
                            }
                        }
                        require(blocks.isNotEmpty() && blocks.map { it.id }.distinct().size == blocks.size) { "blocks must have unique IDs" }
                        blocks
                    } else emptyList()
                    Lesson(
                        lesson.requiredString("id"),
                        lesson.localized("title", language),
                        lesson.localized("objective", language),
                        theory,
                        exercises,
                        xp,
                        concept,
                    )
                }
            }
            require(lessons.isNotEmpty() && lessons.size <= 26) { "lessons must contain 1 to 26 items" }
            require(lessons.map { it.id }.distinct().size == lessons.size) { "duplicate lesson id" }
            val exerciseIds = lessons.flatMap { lesson -> lesson.exercises.map(Exercise::id) }
            require(exerciseIds.distinct().size == exerciseIds.size) { "duplicate exercise id" }
            Course(root.requiredString("id"), language, root.localized("title", language), lessons)
        }
    }

    private fun parseExercise(json: JSONObject, locale: String): Exercise {
        val id = json.requiredString("id")
        val type = json.requiredString("type")
        val visual = json.requiredString("visual")
        require(visual in visuals[type].orEmpty()) { "visual $visual is not supported for $type" }
        require(!json.has("triangle") || json.optJSONObject("triangle") != null) { "triangle must be an object" }
        val triangle = json.optJSONObject("triangle")?.let { diagram ->
            at("triangle") {
                require(diagram.get("angleDegrees") is Number) { "angleDegrees must be a number" }
                val degrees = diagram.getDouble("angleDegrees")
                require(degrees > 0 && degrees < 90) { "angleDegrees must be between 0 and 90" }
                TriangleDiagram(degrees, diagram.requiredString("angleLabel"), diagram.requiredString("base"),
                    diagram.requiredString("opposite"), diagram.requiredString("hypotenuse"))
            }
        }
        val prompt = json.localized("prompt", locale)
        val hint = json.localized("hint", locale)
        val solution = json.getJSONArray("solutionSteps").localizedStrings(locale)
        require(solution.isNotEmpty()) { "solutionSteps must not be empty" }
        return when (type) {
            "multiple_choice" -> {
                val options = json.getJSONArray("options").objects().map { option ->
                    Option(option.requiredString("id"), option.localized("text", locale))
                }
                require(options.size >= 2 && options.map { it.id }.distinct().size == options.size) { "options must have unique IDs" }
                val answer = json.requiredString("correctOptionId")
                require(options.any { it.id == answer }) { "correctOptionId does not exist" }
                if (visual == "angle_builder") require(answer.toIntOrNull()?.let { it in 0..180 } == true) { "angle target must be 0..180" }
                ChoiceExercise(id, visual, prompt, hint, solution, options, answer, triangle)
            }
            "input" -> InputExercise(id, visual, prompt, hint, solution, json.getJSONArray("acceptedAnswers").answers(), triangle)
            "matching" -> {
                val left = json.getJSONArray("leftItems").objects().map { option ->
                    Option(option.requiredString("id"), option.localized("text", locale))
                }
                val right = json.getJSONArray("rightItems").objects().map { option ->
                    val diagram = option.optString("diagram").takeIf(String::isNotBlank)
                    if (visual == "angle_pairs") require(diagram in setOf("acute", "right", "straight")) { "invalid diagram" }
                    Option(option.requiredString("id"), option.localized("text", locale), diagram)
                }
                // @spec spec://modules/learning/PROP-011-course-json-format#visual-rule
                require(left.size == 3 && right.size == 3) { "matching has exactly three cards per column" }
                require(left.map { it.id }.distinct().size == left.size) { "duplicate leftItems id" }
                require(right.map { it.id }.distinct().size == right.size) { "duplicate rightItems id" }
                val pairList = json.getJSONArray("pairs").objects().map { it.requiredString("leftId") to it.requiredString("rightId") }
                val pairs = pairList.toMap()
                require(pairList.size == left.size && pairs.size == left.size) { "pairs must be unique and complete" }
                require(pairs.keys == left.map(Option::id).toSet()) { "pairs reference unknown left item" }
                require(pairs.values.toSet() == right.map(Option::id).toSet()) { "pairs reference unknown or repeated right item" }
                if (visual == "ratio_pairs") require(right.all { '/' in it.text }) { "ratio_pairs items must use /" }
                MatchingExercise(id, visual, prompt, hint, solution, left, right, pairs, triangle)
            }
            "step_by_step" -> {
                val steps = json.getJSONArray("steps").objects().mapIndexed { index, step ->
                    at("steps[$index]") {
                        StepItem(step.localized("prompt", locale), step.getJSONArray("acceptedAnswers").answers(), step.localized("explanation", locale))
                    }
                }
                require(steps.isNotEmpty()) { "steps must not be empty" }
                StepExercise(id, visual, prompt, hint, solution, steps, triangle)
            }
            else -> error("unsupported type $type")
        }
    }

    private fun JSONObject.requiredString(key: String): String = getString(key).also {
        require(it.isNotBlank()) { "$key must not be blank" }
    }

    private fun JSONObject.localized(key: String, locale: String): String {
        val values = getJSONObject(key)
        val gn = values.requiredString("gn-PY")
        val es = values.requiredString("es")
        return if (locale == "es") es else gn
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.localizedStrings(locale: String): List<String> = objects().map { item ->
        val gn = item.requiredString("gn-PY")
        val es = item.requiredString("es")
        if (locale == "es") es else gn
    }
    private fun JSONArray.answers(): List<String> = (0 until length()).map { getString(it) }.also { answers ->
        require(answers.isNotEmpty() && answers.all(String::isNotBlank)) { "acceptedAnswers must not be empty" }
    }

    private inline fun <T> at(path: String, block: () -> T): T = try {
        block()
    } catch (error: Exception) {
        throw IllegalArgumentException("$path: ${error.message}", error)
    }
}
