package com.guarani.mathdemo.course

// @spec spec://modules/learning/PROP-011-course-json-format#package
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

data class Option(val id: String, val text: String, val diagram: String? = null)

sealed interface Exercise {
    val id: String
    val visual: String
    val prompt: String
    val hint: String
    val solutionSteps: List<String>
}

data class ChoiceExercise(
    override val id: String,
    override val visual: String,
    override val prompt: String,
    override val hint: String,
    override val solutionSteps: List<String>,
    val options: List<Option>,
    val correctOptionId: String,
) : Exercise

data class InputExercise(
    override val id: String,
    override val visual: String,
    override val prompt: String,
    override val hint: String,
    override val solutionSteps: List<String>,
    val acceptedAnswers: List<String>,
) : Exercise

data class MatchingExercise(
    override val id: String,
    override val visual: String,
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
    override val visual: String,
    override val prompt: String,
    override val hint: String,
    override val solutionSteps: List<String>,
    val steps: List<StepItem>,
) : Exercise
