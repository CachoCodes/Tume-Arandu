package com.guarani.mathdemo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.guarani.mathdemo.progress.AvatarLook
import org.json.JSONArray
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val avatarPlaceholder = Regex("\\{\\{([^}]+)\\}\\}")
private data class AvatarLayer(val tag: String, val whenFlag: String?, val attrs: Map<String, String>) {
    fun resolve(vals: Map<String, String>): ResolvedAvatarLayer? {
        if (whenFlag != null && vals[whenFlag] != "true") return null
        val resolved = attrs.mapValues { (_, raw) -> avatarPlaceholder.replace(raw) { vals[it.groupValues[1]] ?: "" } }
        return ResolvedAvatarLayer(tag, resolved,
            if (tag == "path") PathParser().parsePathString(resolved["d"] ?: return null).toPath() else null)
    }
}
private data class ResolvedAvatarLayer(val tag: String, val attrs: Map<String, String>, val path: Path?) {
    fun get(name: String): String? = attrs[name]
}

private fun loadAvatarLayers(raw: String): List<AvatarLayer> = JSONArray(raw).let { array ->
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        val attrs = buildMap {
            val keys = item.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key != "tag" && key != "when") put(key, item.getString(key))
            }
        }
        AvatarLayer(item.getString("tag"), item.optString("when").takeIf { it.isNotBlank() && it != "null" }, attrs)
    }
}

private object AvatarTemplate {
    @Volatile private var cached: List<AvatarLayer>? = null
    fun layers(context: android.content.Context): List<AvatarLayer> = cached ?: synchronized(this) {
        cached ?: loadAvatarLayers(context.assets.open("avatar/avatar-template.json").bufferedReader().use { it.readText() })
            .also { cached = it }
    }
}

private fun mix(hex: String, t: Double, white: Boolean): String {
    val rgb = android.graphics.Color.parseColor(hex)
    val target = if (white) 255 else 0
    fun channel(value: Int) = (value + (target - value) * t).roundToInt()
    return "#%02x%02x%02x".format(Locale.US, channel(android.graphics.Color.red(rgb)),
        channel(android.graphics.Color.green(rgb)), channel(android.graphics.Color.blue(rgb)))
}

private fun star(cx: Int, cy: Int, big: Double, small: Double): String = buildString {
    repeat(10) { index ->
        val a = -Math.PI / 2 + index * Math.PI / 5
        val radius = if (index % 2 == 0) big else small
        append(if (index == 0) 'M' else 'L')
        append("%.1f %.1f ".format(Locale.US, cx + radius * cos(a), cy + radius * sin(a)))
    }
    append('Z')
}

private fun avatarVals(look: AvatarLook): Map<String, String> {
    val expr = look.expr
    val brow = when (expr) {
        "surprised", "star" -> "M75 72 Q84 65 92 70 M108 70 Q116 65 125 72"
        "cool" -> "M74 70 Q83 62 92 68 M108 77 Q116 74 125 77"
        else -> "M75 78 Q84 73 92 77 M108 77 Q116 73 125 78"
    }
    val flags = mapOf(
        "hasBg" to (look.bg != "none"), "oTee" to (look.outfit == "tee"), "oHoodie" to (look.outfit == "hoodie"),
        "hBuzz" to (look.hairStyle == "buzz"), "hShort" to (look.hairStyle == "short"),
        "hCurly" to (look.hairStyle == "curly"), "hLong" to (look.hairStyle == "long"),
        "hBun" to (look.hairStyle == "bun" && look.hat !in listOf("cap", "beanie", "grad")),
        "eOpen" to (expr == "smile" || expr == "surprised"), "eHappy" to (expr == "happy"),
        "eWink" to (expr == "wink"), "eCool" to (expr == "cool"), "eStar" to (expr == "star"),
        "mSmile" to (expr == "smile"), "mOpen" to (expr == "happy" || expr == "star"),
        "mSmirk" to (expr == "wink" || expr == "cool"), "mO" to (expr == "surprised"),
        "gRound" to (look.glasses == "round"), "gSun" to (look.glasses == "sun"),
        "gStar" to (look.glasses == "star"), "tCap" to (look.hat == "cap"),
        "tBeanie" to (look.hat == "beanie"), "tPhones" to (look.hat == "phones"),
        "tPiri" to (look.hat == "piri"), "tGrad" to (look.hat == "grad"),
        "tCrown" to (look.hat == "crown"),
    )
    return flags.mapValues { it.value.toString() } + mapOf(
        "bg" to look.bg, "skin" to look.skin, "skinShade" to mix(look.skin, .14, false),
        "hair" to look.hair, "hairHi" to mix(look.hair, .35, true), "hairDark" to mix(look.hair, .25, false),
        "shirt" to look.shirt, "shirtShade" to mix(look.shirt, .2, false), "browD" to brow,
        "eyeRx" to if (expr == "surprised") "7" else "5.5",
        "eyeRy" to if (expr == "surprised") "8" else "6.5",
        "starEyes" to (star(84, 92, 10.0, 4.5) + " " + star(116, 92, 10.0, 4.5)),
        "starLenses" to (star(84, 92, 16.0, 8.0) + " " + star(116, 92, 16.0, 8.0)),
    )
}

private fun svgColor(raw: String?): Color? = raw?.takeIf { it != "none" && it.isNotBlank() }
    ?.let { Color(android.graphics.Color.parseColor(it)) }

private fun DrawScope.drawAvatarLayer(layer: ResolvedAvatarLayer, muted: Boolean) {
    fun v(name: String) = layer.get(name)
    fun n(name: String, default: Float = 0f) = v(name)?.toFloatOrNull() ?: default
    val opacity = n("opacity", 1f)
    fun tint(color: Color?): Color? = color?.let {
        if (!muted) it else {
            val gray = it.red * .299f + it.green * .587f + it.blue * .114f
            Color(it.red*.1f+gray*.9f, it.green*.1f+gray*.9f, it.blue*.1f+gray*.9f, it.alpha)
        }
    }
    val fill = tint(svgColor(v("fill")))?.copy(alpha = opacity * n("fill-opacity", 1f))
    val stroke = tint(svgColor(v("stroke")))?.copy(alpha = opacity * n("stroke-opacity", 1f))
    val style = Stroke(width = n("stroke-width", 1f),
        cap = if (v("stroke-linecap") == "round") StrokeCap.Round else StrokeCap.Butt,
        join = if (v("stroke-linejoin") == "round") StrokeJoin.Round else StrokeJoin.Miter)
    when (layer.tag) {
        "path" -> {
            val path = layer.path ?: return
            fill?.let { drawPath(path, it) }
            stroke?.let { drawPath(path, it, style = style) }
        }
        "circle" -> {
            val center = Offset(n("cx"), n("cy")); val radius = n("r")
            fill?.let { drawCircle(it, radius, center) }
            stroke?.let { drawCircle(it, radius, center, style = style) }
        }
        "ellipse" -> {
            val topLeft = Offset(n("cx") - n("rx"), n("cy") - n("ry"))
            val size = Size(n("rx") * 2, n("ry") * 2)
            fill?.let { drawOval(it, topLeft, size) }
            stroke?.let { drawOval(it, topLeft, size, style = style) }
        }
        "rect" -> {
            val topLeft = Offset(n("x"), n("y")); val size = Size(n("width"), n("height"))
            val corner = CornerRadius(n("rx"), n("ry", n("rx")))
            fill?.let { drawRoundRect(it, topLeft, size, corner) }
            stroke?.let { drawRoundRect(it, topLeft, size, corner, style) }
        }
    }
}

// @spec spec://modules/android/PROP-010-android-demo-architecture#navigation
@Composable
fun AvatarView(look: AvatarLook, size: Dp, modifier: Modifier = Modifier, muted: Boolean = false) {
    val context = LocalContext.current
    val layers = remember(context) { AvatarTemplate.layers(context) }
    val resolved = remember(look, layers) {
        val vals = avatarVals(look)
        layers.mapNotNull { it.resolve(vals) }
    }
    Canvas(modifier.size(size).clip(CircleShape).graphicsLayer { alpha = if (muted) .55f else 1f }) {
        withTransform({ scale(this@Canvas.size.width / 200f, this@Canvas.size.height / 200f, pivot = Offset.Zero) }) {
            resolved.forEach { drawAvatarLayer(it, muted) }
        }
    }
}
