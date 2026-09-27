package com.guarani.mathdemo.ui

import android.content.Context
import com.guarani.mathdemo.course.Course
import com.guarani.mathdemo.progress.AvatarLook
import com.guarani.mathdemo.progress.ProgressSnapshot
import org.json.JSONObject

internal data class AvatarUnlock(val kind: String, val n: Int = 0, val topic: String = "", val chip: String = "")
internal data class AvatarItem(val id: String, val label: String, val unlock: AvatarUnlock, val countsInCollection: Boolean)
internal data class AvatarColor(val hex: String, val label: String)
internal data class AvatarColors(val field: String, val title: String, val list: List<AvatarColor>)
internal data class AvatarCategory(val id: String, val tab: String, val title: String, val field: String,
    val items: List<AvatarItem>, val colors: AvatarColors?)

internal class AvatarCatalog(context: Context) {
    private val json = JSONObject(context.assets.open("avatar/unlock-rules.json").bufferedReader().use { it.readText() })
    private val topicLessonIds = json.getJSONObject("topicLessonIds")
    val categories: List<AvatarCategory> = json.getJSONArray("categories").let { array ->
        (0 until array.length()).map { index ->
            val cat = array.getJSONObject(index)
            val items = cat.getJSONArray("items").let { values ->
                (0 until values.length()).map { itemIndex ->
                    val item = values.getJSONObject(itemIndex)
                    val req = item.getJSONObject("unlock")
                    AvatarItem(item.getString("id"), item.getString("label"),
                        AvatarUnlock(req.getString("kind"), req.optInt("n"), req.optString("topic"), req.optString("chip")),
                        item.optBoolean("countsInCollection", true))
                }
            }
            val colors = cat.optJSONObject("colors")?.let { data ->
                AvatarColors(data.getString("lookField"), data.getString("title"), data.getJSONArray("list").let { list ->
                    (0 until list.length()).map { colorIndex ->
                        val pair = list.getJSONArray(colorIndex); AvatarColor(pair.getString(0), pair.getString(1))
                    }
                })
            }
            AvatarCategory(cat.getString("id"), cat.getString("tab"), cat.getString("title"), cat.getString("lookField"), items, colors)
        }
    }
    val chipStyles: Map<String, Pair<String, String>> = json.getJSONObject("chipStyles").let { styles ->
        buildMap {
            val keys = styles.keys()
            while (keys.hasNext()) {
                val key = keys.next(); val value = styles.getJSONObject(key)
                put(key, value.getString("bg") to value.getString("fg"))
            }
        }
    }

    fun unlocked(item: AvatarItem, progress: ProgressSnapshot, course: Course): Boolean = when (item.unlock.kind) {
        "free" -> true
        "xp" -> progress.xp >= item.unlock.n
        "lessons" -> progress.completedLessonIds.size >= item.unlock.n
        "topic" -> topicLessonIds.optJSONArray(item.unlock.topic)?.let { ids ->
            ids.length() > 0 && (0 until ids.length()).all { ids.getString(it) in progress.completedLessonIds }
        } ?: false
        "stage" -> course.lessons.isNotEmpty() && course.lessons.all { it.id in progress.completedLessonIds }
        else -> false
    }

    fun count(progress: ProgressSnapshot, course: Course): Pair<Int, Int> = categories.flatMap { it.items }
        .filter { it.countsInCollection }.let { items -> items.count { unlocked(it, progress, course) } to items.size }

    fun nextReward(progress: ProgressSnapshot, course: Course): Pair<AvatarCategory, AvatarItem>? =
        categories.flatMap { cat -> cat.items.map { cat to it } }
            .filter { (_, item) -> item.unlock.kind == "xp" && !unlocked(item, progress, course) }
            .minByOrNull { (_, item) -> item.unlock.n }

    fun progress(item: AvatarItem, snapshot: ProgressSnapshot, course: Course): Pair<Int, String> {
        val req = item.unlock
        return when (req.kind) {
            "xp" -> ((snapshot.xp * 100 / req.n).coerceIn(0, 100)) to "${snapshot.xp} / ${req.n} XP"
            "lessons" -> ((snapshot.completedLessonIds.size * 100 / req.n).coerceIn(0, 100)) to "${snapshot.completedLessonIds.size} / ${req.n} lecciones"
            "stage" -> ((course.lessons.count { it.id in snapshot.completedLessonIds } * 100 / course.lessons.size.coerceAtLeast(1)).coerceIn(0, 100)) to "${course.lessons.count { it.id in snapshot.completedLessonIds }} / ${course.lessons.size} lecciones"
            "topic" -> 0 to req.chip
            else -> 100 to ""
        }
    }

    fun randomLook(progress: ProgressSnapshot, course: Course): AvatarLook {
        var look = AvatarLook()
        categories.forEach { category ->
            category.items.filter { unlocked(it, progress, course) }.randomOrNull()?.let { look = look.with(category.field, it.id) }
            category.colors?.let { colors -> look = look.with(colors.field, colors.list.random().hex) }
        }
        return look
    }
}
