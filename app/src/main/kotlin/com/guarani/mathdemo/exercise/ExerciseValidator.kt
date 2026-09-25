package com.guarani.mathdemo.exercise

import com.guarani.mathdemo.course.ChoiceExercise
import com.guarani.mathdemo.course.Exercise
import com.guarani.mathdemo.course.InputExercise
import com.guarani.mathdemo.course.MatchingExercise
import com.guarani.mathdemo.course.StepExercise
import java.text.Normalizer
import java.util.Locale

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
fun normalizeAnswer(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFC)
    .trim()
    .lowercase(Locale.ROOT)
    .replace(Regex("\\s+"), " ")

fun validateAnswer(exercise: Exercise, answer: String): Boolean = when (exercise) {
    is ChoiceExercise -> answer == exercise.correctOptionId
    is InputExercise -> exercise.acceptedAnswers.any { normalizeAnswer(it) == normalizeAnswer(answer) }
    is MatchingExercise -> false
    is StepExercise -> validateStep(exercise, 0, answer)
}

fun validateMatching(exercise: MatchingExercise, answers: Map<String, String>): Boolean =
    answers.size == exercise.pairs.size && answers == exercise.pairs

fun validateStep(exercise: StepExercise, index: Int, answer: String): Boolean =
    exercise.steps.getOrNull(index)?.acceptedAnswers?.any { normalizeAnswer(it) == normalizeAnswer(answer) } == true
