package com.guarani.mathdemo.progress

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.guarani.mathdemo.course.Lesson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

// @spec spec://modules/android/PROP-010-android-demo-architecture#persistence
private val Context.progressDataStore by preferencesDataStore(name = "learning_progress")
// Preview mode keeps its answers apart from the learner's real progress.
private val Context.previewDataStore by preferencesDataStore(name = "exercise_preview")
private val SNAPSHOT_KEY = stringPreferencesKey("snapshot_v1")
private val HELP_TIP_DISMISSED_KEY = booleanPreferencesKey("help_tip_dismissed_v1")

data class ExerciseResult(
    val answer: String? = null,
    val correct: Boolean = false,
    val solutionViewed: Boolean = false,
    val attempts: Int = 0,
    val stepIndex: Int = 0,
) {
    val isTerminal: Boolean get() = correct || solutionViewed
}

data class ProgressSnapshot(
    val completedLessonIds: Set<String> = emptySet(),
    val completedBookIds: Set<String> = emptySet(),
    val bookReveal: Map<String, Int> = emptyMap(),
    val results: Map<String, ExerciseResult> = emptyMap(),
    val currentLessonId: String? = null,
    val currentExerciseId: String? = null,
    val xp: Int = 0,
    val avatar: AvatarLook = AvatarLook(),
)

data class AvatarLook(
    val skin: String = "#f4c29a", val hair: String = "#5a3a22", val hairStyle: String = "short",
    val expr: String = "smile", val hat: String = "cap", val glasses: String = "none",
    val outfit: String = "tee", val shirt: String = "#2d63d6", val bg: String = "#dfe8fb",
) {
    fun value(field: String): String = when (field) {
        "skin" -> skin; "hair" -> hair; "hairStyle" -> hairStyle; "expr" -> expr
        "hat" -> hat; "glasses" -> glasses; "outfit" -> outfit; "shirt" -> shirt; else -> bg
    }
    fun with(field: String, value: String): AvatarLook = when (field) {
        "skin" -> copy(skin = value); "hair" -> copy(hair = value); "hairStyle" -> copy(hairStyle = value)
        "expr" -> copy(expr = value); "hat" -> copy(hat = value); "glasses" -> copy(glasses = value)
        "outfit" -> copy(outfit = value); "shirt" -> copy(shirt = value); else -> copy(bg = value)
    }
}

sealed interface ProgressState {
    data object Loading : ProgressState
    data class Ready(val snapshot: ProgressSnapshot) : ProgressState
    data object Unavailable : ProgressState
}

class ProgressRepository(context: Context, preview: Boolean = false) {
    private val store = context.applicationContext.let { if (preview) it.previewDataStore else it.progressDataStore }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    // @spec spec://modules/android/PROP-010-android-demo-architecture#persistence
    val helpTipDismissed: Flow<Boolean> = store.data
        .map { it[HELP_TIP_DISMISSED_KEY] ?: false }
        .catch { emit(false) }

    val progress: Flow<ProgressState> = store.data
        .map<Preferences, ProgressState> { preferences ->
            ProgressState.Ready(preferences[SNAPSHOT_KEY]?.let(::decode) ?: ProgressSnapshot())
        }
        .catch { emit(ProgressState.Unavailable) }

    suspend fun saveCursor(lessonId: String, exerciseId: String) {
        update { it.copy(currentLessonId = lessonId, currentExerciseId = exerciseId) }
    }

    // @spec spec://modules/learning/FEAT-010-learning-demo#progress
    suspend fun completeBook(lessonId: String) {
        update { it.copy(completedBookIds = it.completedBookIds + lessonId) }
    }

    suspend fun saveBookReveal(lessonId: String, index: Int) {
        update { it.copy(bookReveal = it.bookReveal + (lessonId to maxOf(index, it.bookReveal[lessonId] ?: 0))) }
    }

    suspend fun saveAvatar(look: AvatarLook) { update { it.copy(avatar = look) } }

    // @spec spec://modules/android/PROP-010-android-demo-architecture#persistence
    suspend fun dismissHelpTip() {
        store.edit { it[HELP_TIP_DISMISSED_KEY] = true }
    }

    suspend fun recordAnswer(exerciseId: String, answer: String, correct: Boolean, completedStepIndex: Int? = null) {
        update { current ->
            val previous = current.results[exerciseId] ?: ExerciseResult()
            current.copy(results = current.results + (exerciseId to previous.copy(
                answer = answer,
                correct = previous.correct || correct,
                attempts = previous.attempts + 1,
                stepIndex = completedStepIndex ?: previous.stepIndex,
            )))
        }
    }

    suspend fun saveStepProgress(exerciseId: String, answer: String, nextStepIndex: Int) {
        update { current ->
            val previous = current.results[exerciseId] ?: ExerciseResult()
            current.copy(results = current.results + (exerciseId to previous.copy(
                answer = answer,
                attempts = previous.attempts + 1,
                stepIndex = nextStepIndex,
            )))
        }
    }

    suspend fun showSolution(exerciseId: String) {
        update { current ->
            val previous = current.results[exerciseId] ?: ExerciseResult()
            current.copy(results = current.results + (exerciseId to previous.copy(solutionViewed = true)))
        }
    }

    suspend fun completeLesson(lesson: Lesson) {
        update { current ->
            val ready = lesson.exercises.all { current.results[it.id]?.isTerminal == true }
            require(ready) { "Cannot complete a lesson with unfinished exercises" }
            val firstCompletion = lesson.id !in current.completedLessonIds
            current.copy(
                completedLessonIds = current.completedLessonIds + lesson.id,
                xp = current.xp + if (firstCompletion) lesson.xpReward else 0,
                currentLessonId = null,
                currentExerciseId = null,
            )
        }
    }

    private suspend fun update(change: (ProgressSnapshot) -> ProgressSnapshot) {
        store.edit { preferences ->
            val current = preferences[SNAPSHOT_KEY]?.let(::decode) ?: ProgressSnapshot()
            preferences[SNAPSHOT_KEY] = encode(change(current))
        }
    }

    private fun encode(snapshot: ProgressSnapshot): String = JSONObject().apply {
        put("schemaVersion", 1)
        put("completedLessonIds", JSONArray(snapshot.completedLessonIds.toList()))
        put("completedBookIds", JSONArray(snapshot.completedBookIds.toList()))
        put("bookReveal", JSONObject(snapshot.bookReveal))
        put("currentLessonId", snapshot.currentLessonId)
        put("currentExerciseId", snapshot.currentExerciseId)
        put("xp", snapshot.xp)
        put("avatar", JSONObject().apply {
            listOf("skin", "hair", "hairStyle", "expr", "hat", "glasses", "outfit", "shirt", "bg")
                .forEach { put(it, snapshot.avatar.value(it)) }
        })
        put("results", JSONObject().apply {
            snapshot.results.forEach { (id, result) -> put(id, JSONObject().apply {
                put("answer", result.answer)
                put("correct", result.correct)
                put("solutionViewed", result.solutionViewed)
                put("attempts", result.attempts)
                put("stepIndex", result.stepIndex)
            }) }
        })
    }.toString()

    private fun decode(raw: String): ProgressSnapshot {
        val json = JSONObject(raw)
        require(json.getInt("schemaVersion") == 1) { "Unsupported progress snapshot version" }
        val completed = json.optJSONArray("completedLessonIds")?.let { array ->
            (0 until array.length()).map(array::getString).toSet()
        }.orEmpty()
        val completedBooks = json.optJSONArray("completedBookIds")?.let { array ->
            (0 until array.length()).map(array::getString).toSet()
        }.orEmpty()
        val bookReveal = json.optJSONObject("bookReveal")?.let { saved ->
            buildMap { val keys = saved.keys(); while (keys.hasNext()) { val key = keys.next(); put(key, saved.optInt(key).coerceAtLeast(0)) } }
        }.orEmpty()
        val savedResults = json.optJSONObject("results") ?: JSONObject()
        val results = buildMap {
            val keys = savedResults.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val value = savedResults.getJSONObject(id)
                put(id, ExerciseResult(
                    answer = value.optString("answer").takeUnless { it == "null" },
                    correct = value.optBoolean("correct"),
                    solutionViewed = value.optBoolean("solutionViewed"),
                    attempts = value.optInt("attempts"),
                    stepIndex = value.optInt("stepIndex"),
                ))
            }
        }
        return ProgressSnapshot(
            completedLessonIds = completed,
            completedBookIds = completedBooks,
            bookReveal = bookReveal,
            results = results,
            currentLessonId = json.optString("currentLessonId").takeUnless { it == "null" },
            currentExerciseId = json.optString("currentExerciseId").takeUnless { it == "null" },
            xp = json.optInt("xp"),
            avatar = json.optJSONObject("avatar")?.let { saved ->
                val default = AvatarLook()
                listOf("skin", "hair", "hairStyle", "expr", "hat", "glasses", "outfit", "shirt", "bg")
                    .fold(default) { look, field -> look.with(field, saved.optString(field, default.value(field))) }
            } ?: AvatarLook(),
        )
    }
}
