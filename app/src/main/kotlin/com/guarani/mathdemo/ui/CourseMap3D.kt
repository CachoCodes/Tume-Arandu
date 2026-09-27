package com.guarani.mathdemo.ui

import android.app.ActivityManager
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guarani.mathdemo.course.Course
import com.guarani.mathdemo.course.Lesson
import com.guarani.mathdemo.progress.ProgressSnapshot
import com.guarani.mathdemo.progress.AvatarLook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.SoftReference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import android.graphics.Canvas as NCanvas
import android.graphics.Color as NColor

// Port of the visual mock guaranimath-map-3d.html (iteration 27 · 3D).
// One world, one camera: every scene point is world (x, y, z); UI chrome alone is screen space.
// Scene units are the mock's 390 × 864 phone; the canvas scales them to the device width.

private data class V(val x: Double, val y: Double, val z: Double = 0.0) {
    operator fun plus(o: V) = V(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V) = V(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Double) = V(x * k, y * k, z * k)
    operator fun unaryMinus() = V(-x, -y, -z)
    infix fun dot(o: V) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: V) = V(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun norm(): V = this * (1 / sqrt(this dot this))
    fun rotZ(a: Double) = V(x * cos(a) - y * sin(a), x * sin(a) + y * cos(a), z)
    fun up(h: Double) = V(x, y, z + h)
}

private data class P(val x: Double, val y: Double) {
    operator fun minus(o: P) = P(x - o.x, y - o.y)
}

private fun rad(degrees: Double) = degrees * PI / 180

private object Cam {
    val ca = cos(rad(45.0)); val sa = sin(rad(45.0)); val ce = cos(rad(40.0)); val se = sin(rad(40.0))
    val scale = sqrt(1.5)
    const val OX = 195.0
    const val OY = 429.0
    const val DIST = 2400.0
    val RIGHT = V(ca, -sa); val UP = V(-sa * se, -ca * se, ce); val VIEW = V(sa * ce, ca * ce, se); val TOWARD = V(sa, ca)

    // Layout is authored in "map pixels" (x across, y down the scrolling map) and dropped onto the ground once.
    fun mapToWorld(px: Double, py: Double): V {
        val x = (px - OX) / scale
        val y = (py - OY) / (scale * se)
        return V(ca * x + sa * y, -sa * x + ca * y)
    }

    fun mapToScreen0(p: V) = P((p dot V(ca, -sa)) * scale + OX, (p dot V(sa, ca)) * scale * se + OY)
}

private class View3(val scroll: Double) {
    val target = Cam.mapToWorld(Cam.OX, Cam.OY + scroll)
    val eye = target + Cam.VIEW * Cam.DIST
    fun toCamera(p: V): V { val r = p - target; return V(r dot Cam.RIGHT, r dot Cam.UP, r dot Cam.VIEW) }
    fun project(p: V): P {
        val c = toCamera(p)
        val k = Cam.DIST / (Cam.DIST - c.z)
        return P(Cam.OX + Cam.scale * k * c.x, Cam.OY - Cam.scale * k * c.y)
    }
    fun depth(p: V) = toCamera(p).z
    // A face is visible when it turns towards the eye.
    fun faces(normal: V, at: V) = (normal dot (eye - at)) > 1e-9
}

// Key light upper-left, a little in front of the scene: left faces lit, shadows fall back-right.
private val LIGHT: V = run {
    val e = rad(55.0)
    ((Cam.RIGHT * -.62 + Cam.TOWARD * .45).norm() * cos(e) + V(0.0, 0.0, sin(e))).norm()
}
private const val AMBIENT = .7
private const val DIFFUSE = .3
private val TOP_LIGHT = AMBIENT + DIFFUSE * LIGHT.z
private fun intensity(n: V) = (AMBIENT + DIFFUSE * max(0.0, n dot LIGHT)) / TOP_LIGHT
private fun jsRound(v: Double) = floor(v + .5).toInt()
private fun tint(rgb: Int, k: Double): Int {
    fun ch(shift: Int) = min(255, jsRound(((rgb shr shift) and 255) * k))
    return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}
private fun castShadow(p: V, planeZ: Double = 0.0) =
    V(p.x - (p.z - planeZ) * LIGHT.x / LIGHT.z, p.y - (p.z - planeZ) * LIGHT.y / LIGHT.z, planeZ)

private fun hull(input: List<P>): List<P> {
    val s = input.sortedWith(compareBy<P>({ it.x }, { it.y }))
    fun cross(o: P, a: P, b: P) = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
    val lo = ArrayList<P>(); val up = ArrayList<P>()
    for (p in s) { while (lo.size > 1 && cross(lo[lo.size - 2], lo.last(), p) <= 0) lo.removeAt(lo.size - 1); lo.add(p) }
    for (p in s.asReversed()) { while (up.size > 1 && cross(up[up.size - 2], up.last(), p) <= 0) up.removeAt(up.size - 1); up.add(p) }
    return lo.dropLast(1) + up.dropLast(1)
}

private const val SEGMENTS = 56
private fun ring(c: V, r: Double, z: Double) = List(SEGMENTS) {
    val t = it * 2 * PI / SEGMENTS
    c + V(r * cos(t), r * sin(t), z)
}
private fun centroid(ps: List<V>) = ps.reduce { a, b -> a + b } * (1.0 / ps.size)

private const val SHADOW = 0x1a4210
private const val PLINTH = 0xf4f1ea
private val SOFT = BlurMaskFilter(2.25f, BlurMaskFilter.Blur.NORMAL)
private val CONTACT = BlurMaskFilter(1.21f, BlurMaskFilter.Blur.NORMAL)

private fun weight(w: Int, italic: Boolean = false): Typeface =
    if (Build.VERSION.SDK_INT >= 28) Typeface.create(Typeface.DEFAULT, w, italic)
    else Typeface.create(Typeface.DEFAULT, if (italic) Typeface.BOLD_ITALIC else Typeface.BOLD)
private val LABEL_FACE = weight(650)
private val WORD_FACE = weight(800)
private val TAG_FACE = weight(700, true)

private fun argb(rgb: Int, alpha: Double = 1.0) = (jsRound(alpha.coerceIn(0.0, 1.0) * 255) shl 24) or rgb

private typealias Op = (NCanvas) -> Unit

private fun pathOf(ps: List<P>, close: Boolean = true) = Path().apply {
    ps.forEachIndexed { i, p -> if (i == 0) moveTo(p.x.toFloat(), p.y.toFloat()) else lineTo(p.x.toFloat(), p.y.toFloat()) }
    if (close) close()
}

private fun fill(rgb: Int, alpha: Double = 1.0, blur: BlurMaskFilter? = null, shader: Shader? = null) =
    Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = argb(rgb, alpha); maskFilter = blur; this.shader = shader }

private fun strokePaint(rgb: Int, width: Double, alpha: Double = 1.0, dash: FloatArray? = null, blur: BlurMaskFilter? = null) =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = argb(rgb, alpha); strokeWidth = width.toFloat()
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        if (dash != null) { pathEffect = DashPathEffect(dash, 0f); strokeCap = Paint.Cap.BUTT }
        maskFilter = blur
    }

// SVG matrix(a b c d e f) of a local affine frame at world point p: drawing units map to world axes a and b.
private fun frameMatrix(o: P, u: P, v: P) = Matrix().apply {
    setValues(floatArrayOf(u.x.toFloat(), v.x.toFloat(), o.x.toFloat(), u.y.toFloat(), v.y.toFloat(), o.y.toFloat(), 0f, 0f, 1f))
}

private fun text(c: NCanvas, s: String, x: Double, y: Double, size: Double, rgb: Int, face: Typeface, align: Paint.Align = Paint.Align.CENTER, alpha: Double = 1.0, blur: BlurMaskFilter? = null, stroke: Pair<Int, Double>? = null) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = face; textSize = size.toFloat(); textAlign = align; color = argb(rgb, alpha); maskFilter = blur }
    if (stroke != null) {
        val sp = Paint(p).apply { style = Paint.Style.STROKE; color = argb(stroke.first, alpha); strokeWidth = stroke.second.toFloat(); strokeJoin = Paint.Join.ROUND }
        c.drawText(s, x.toFloat(), y.toFloat(), sp)
    }
    c.drawText(s, x.toFloat(), y.toFloat(), p)
}

// ---------- Layout: seven screens, one winding path of book → lesson → review triplets ----------
private const val SCREEN = 864.0
private const val STAGE_TOP = 226.0
private const val STAGE_BOTTOM = 600.0
private const val TROPHY_Y = 688.0
private const val CENTER_X = 195.0
private const val RADIUS = 30.0
private const val BOOK_RADIUS = 27.0
private const val REVIEW_RADIUS = 21.0
private const val TILE_HEIGHT = 8.0

private class StageDef(val dir: Int, val amp: Double, val types: String)
private val STAGES = List(6) { StageDef(if (it % 2 == 0) 1 else -1, 130.0, "BLRBLR") } + StageDef(1, 130.0, "BLR")
private class MapNode(val stage: Int, val k: Int, val type: Char, val n: Int) {
    val lesson get() = type == 'L'
    val book get() = type == 'B'
}
private val NODES = STAGES.flatMapIndexed { s, st -> st.types.mapIndexed { k, t -> MapNode(s, k, t, st.types.length) } }
private val COURSE_SLOTS = NODES.indices.filter { !NODES[it].book }
private val MAP_HEIGHT = STAGES.size * SCREEN
private fun mapX(i: Int): Double {
    val n = NODES[i]
    val stage = STAGES[n.stage]
    val phase = if (n.n == 3) n.k * PI / 2 else n.k * 2 * PI / (n.n - 1)
    return CENTER_X + stage.dir * stage.amp * sin(phase)
}
private fun mapY(i: Int): Double {
    val n = NODES[i]
    return n.stage * SCREEN + if (n.n == 3) 290.0 + n.k * 140.0 else STAGE_TOP + n.k * (STAGE_BOTTOM - STAGE_TOP) / 5
}
private val LESSONS = NODES.indices.map { Cam.mapToWorld(mapX(it), mapY(it)) }
private fun radiusOf(i: Int) = when { NODES[i].lesson -> RADIUS; NODES[i].book -> BOOK_RADIUS; else -> REVIEW_RADIUS }
private fun lessonNumber(i: Int) = (0..i).count { NODES[it].lesson }

// Bays: the free side next to each hump of the snake (upper hump ≈ node 2, lower hump ≈ node 6).
private fun bay(s: Int, upper: Boolean): Pair<Double, Double> {
    val st = STAGES[s]; val base = NODES.indexOfFirst { it.stage == s }
    val side = if (upper) -st.dir else st.dir
    return (CENTER_X + side * st.amp) to mapY(base + if (upper) 1 else 4)
}

private enum class Kind { PRISM, SEN30, LIGHTHOUSE, KITE, LADDER, TAN45, TROPHY }
private class Plinth(
    val round: Boolean, val h: Double, val r: Double = 0.0, val shift: Double = 0.0,
    var length: Double = 0.0, val depth: Double = 0.0, val back: Double = 0.0,
    val alongRun: Boolean = false, val wall: Boolean = false, var offset: Double = 0.0,
)
private class Prop(val kind: Kind, val atX: Double, val atY: Double, val yaw: Double, val tall: Double, val plinth: Plinth?, val center: V = Cam.mapToWorld(atX, atY)) {
    fun lifted(h: Double) = Prop(kind, atX, atY, yaw, tall, plinth, center.up(h))
    val stage = floor(atY / SCREEN).toInt()
    val k = if (kind == Kind.TROPHY) 9.0 else (atY - stage * SCREEN - STAGE_TOP) / ((STAGE_BOTTOM - STAGE_TOP) / 8)
}

private const val KITE_RUN = 62.0
private const val KITE_HEIGHT = 58.0
private const val PRISM_RADIUS = 58.0

private val PROPS: List<Prop> = run {
    val u0 = bay(0, true); val l1 = bay(1, false); val u1 = bay(1, true); val u2 = bay(2, true)
    listOf(
        Prop(Kind.PRISM, u0.first - 12, u0.second + 4, rad(-65.0), 62.0, null),
        Prop(Kind.LIGHTHOUSE, l1.first - 30, l1.second - 8, rad(18.0), 76.0, Plinth(true, 6.0, r = 42.0, shift = 24.0)),
        Prop(Kind.KITE, u1.first - 52, u1.second + 30, rad(12.0), 90.0, Plinth(false, 5.0, length = 78.0, depth = 14.0, alongRun = true)),
        Prop(Kind.LADDER, u2.first - 20, u2.second - 4, rad(38.0), 56.0, Plinth(false, 5.0, wall = true)),
    ) + STAGES.indices.map { s -> Prop(Kind.TROPHY, CENTER_X, s * SCREEN + TROPHY_Y, 0.0, 55.0, Plinth(true, 5.0, r = 22.0)) }
}.onEach { p ->
    // Letter plinths fit the real width of the lettering (+6 each side).
    val paint = Paint().apply { typeface = WORD_FACE; textSize = 20f }
    when (p.kind) {
        Kind.SEN30 -> { val w = paint.measureText("sen 30° ="); val x = (14 - w + 30) * 1.22; val width = (36.5 - 14 + w) * 1.22; p.plinth!!.length = width + 12; p.plinth.offset = x + width / 2 }
        Kind.TAN45 -> { val w = paint.measureText("tg 45° = 1") * 1.22; p.plinth!!.length = w + 12; p.plinth.offset = 0.0 }
        else -> Unit
    }
}

private class SlabGeometry(val bottom: List<V>, val top: List<V>, val faces: List<Face>)
private class Face(val ps: List<V>, val n: V? = null, val matl: Int? = null)

private fun slabGeometry(c: V, axisL: V, axisD: V, l: Double, d: Double, h: Double): SlabGeometry {
    fun q(sl: Int, sd: Int, z: Double) = c + axisL * (sl * l / 2) + axisD * (sd * d / 2) + V(0.0, 0.0, z)
    val bottom = listOf(q(-1, -1, 0.0), q(1, -1, 0.0), q(1, 1, 0.0), q(-1, 1, 0.0))
    val top = bottom.map { it.up(h) }
    return SlabGeometry(bottom, top, listOf(Face(top)) + (0..3).map { Face(listOf(bottom[it], bottom[(it + 1) % 4], top[(it + 1) % 4], top[it])) })
}

private fun slabCenter(p: Prop): Triple<V, Double, Double> {
    val pl = p.plinth!!
    var c = p.center
    val axisL = Cam.RIGHT.rotZ(p.yaw); val axisD = Cam.TOWARD.rotZ(p.yaw)
    var l = pl.length; var d = pl.depth
    if (pl.back != 0.0) c = c + axisD * -pl.back
    if (pl.alongRun) c = c + axisL * (KITE_RUN / 2)
    if (pl.offset != 0.0) c = c + axisL * pl.offset
    if (pl.wall) { val run = 52 / tan(rad(60.0)); val t = 13.0; l = t + run + 12; d = 58.0; c = c + axisL * ((run + t / 2) / 2 + 1) }
    return Triple(c, l, d)
}

private fun plinthBottom(p: Prop): List<V> {
    val pl = p.plinth ?: return ring(p.center, PRISM_RADIUS, 0.0)
    if (pl.round) return ring(p.center + Cam.RIGHT.rotZ(p.yaw).norm() * pl.shift, pl.r, 0.0)
    val (c, l, d) = slabCenter(p)
    return slabGeometry(c, Cam.RIGHT.rotZ(p.yaw), Cam.TOWARD.rotZ(p.yaw), l, d, pl.h).bottom
}

// ---------- Progress mapped onto the stage nodes ----------
private class MapProgress(val lessonAt: List<Lesson?>, val done: Int) {
    val current get() = done
    fun stageDone(s: Int) = NODES.indices.filter { NODES[it].stage == s }.all { it < done }
    fun stageCount(s: Int) = NODES.indices.filter { NODES[it].stage == s && lessonAt[it] != null }.let { ids -> ids.count { it < done } to ids.size }
}

private fun mapProgress(course: Course, progress: ProgressSnapshot): MapProgress {
    val lessonAt = MutableList<Lesson?>(NODES.size) { null }
    COURSE_SLOTS.forEachIndexed { slot, node -> lessonAt[node] = course.lessons.getOrNull(slot) }
    NODES.indices.filter { NODES[it].book }.forEach { book ->
        val next = NODES.indices.first { it > book && NODES[it].stage == NODES[book].stage && NODES[it].lesson }
        lessonAt[book] = lessonAt[next]?.takeIf { it.concept.isNotEmpty() }
    }
    // Empty decorative nodes inherit the previous lesson's state.
    var lastLessonDone = false
    val flags = NODES.mapIndexed { i, _ ->
        if (NODES[i].book) {
            val id = lessonAt[i]?.id
            id == null || id in progress.completedBookIds || id in progress.completedLessonIds
        } else {
            if (lessonAt[i] != null) lastLessonDone = lessonAt[i]!!.id in progress.completedLessonIds
            lastLessonDone
        }
    }
    val done = flags.indexOfFirst { !it }.let { if (it < 0) NODES.size else it }
    return MapProgress(lessonAt, done)
}

// ---------- Scene builder for one camera position ----------
private class Frame(val ground: List<Op>, val objects: List<Op>, val air: Op?, val pivot: P?, val lamp: P?, val lampOpacity: Double)
private class PropOut(val ground: List<Op>, val svg: List<Op>, val air: List<Op>? = null, val pivot: P? = null, val lamp: P? = null)

private val REPEAT_PATH: Path by lazy {
    Path().apply {
        listOf("M-7 -1.5A7.2 7.2 0 0 1 5.6 -4.6", "M6.4 -9.2 5.9 -4.2 1.2 -5.6", "M7 1.5A7.2 7.2 0 0 1 -5.6 4.6", "M-6.4 9.2 -5.9 4.2 -1.2 5.6")
            .forEach { addPath(PathParser().parsePathString(it).toPath().asAndroidPath()) }
    }
}
private val BOOK_PATH: Path by lazy {
    Path().apply {
        moveTo(-13f, -8f); lineTo(-3f, -7f); lineTo(0f, -4f); lineTo(3f, -7f); lineTo(13f, -8f)
        lineTo(13f, 9f); lineTo(3f, 10f); lineTo(0f, 13f); lineTo(-3f, 10f); lineTo(-13f, 9f); close()
        moveTo(0f, -4f); lineTo(0f, 13f)
    }
}
private val STAR_PATH: Path by lazy {
    pathOf(List(10) { k -> val r = if (k % 2 == 1) 7.5 else 17.0; val t = -PI / 2 + k * PI / 5; P(r * cos(t), r * sin(t)) })
}

private class Builder(val v: View3, val calm: Boolean, val state: MapProgress) {
    var shadowPlane = 0.0

    fun project(p: V) = v.project(p)
    fun poly2(ps: List<P>, rgb: Int, alpha: Double = 1.0, blur: BlurMaskFilter? = null, shader: Shader? = null, stroke: Int? = null, strokeWidth: Double = 0.0, strokeAlpha: Double = 1.0): Op {
        val path = pathOf(ps)
        val f = fill(rgb, alpha, blur, shader)
        val s = stroke?.let { strokePaint(it, strokeWidth, strokeAlpha) }
        return { c -> c.drawPath(path, f); if (s != null) c.drawPath(path, s) }
    }
    fun poly(ps: List<V>, rgb: Int, alpha: Double = 1.0, blur: BlurMaskFilter? = null, stroke: Int? = null, strokeWidth: Double = 0.0, strokeAlpha: Double = 1.0) =
        poly2(ps.map(::project), rgb, alpha, blur, null, stroke, strokeWidth, strokeAlpha)
    fun line3(ps: List<V>, rgb: Int, width: Double, alpha: Double = 1.0, dash: FloatArray? = null, blur: BlurMaskFilter? = null): Op {
        val path = pathOf(ps.map(::project), close = false)
        val paint = strokePaint(rgb, width, alpha, dash, blur)
        return { c -> c.drawPath(path, paint) }
    }
    fun lineShadow(ps: List<V>, width: Double) = line3(ps.map { castShadow(it, shadowPlane) }, SHADOW, width, .13, blur = SOFT)
    fun framed(p: V, a: V, b: V, draw: (NCanvas) -> Unit): Op {
        val o = project(p); val m = frameMatrix(o, project(p + a) - o, project(p + b) - o)
        return { c -> c.save(); c.concat(m); draw(c); c.restore() }
    }
    fun label(c: NCanvas, x: Double, y: Double, s: String, size: Double, rgb: Int, alpha: Double = 1.0) = text(c, s, x, y, size, rgb, LABEL_FACE, alpha = alpha)
    // Screen-facing label pinned to a world point (for letters like h, θ that name parts of a figure).
    fun tag(p: V, t: String, size: Double, rgb: Int, dx: Double = 0.0, dy: Double = 0.0): Op {
        val q = project(p)
        return { c -> text(c, t, q.x + dx, q.y + dy + size * .35, size, rgb, TAG_FACE) }
    }

    // Shadow = hull of the solid dropped along the light, fading from the contact (dark, sharp) to the tip (light).
    fun shadowShape(vertices: List<V>, from: V, to: V, planeZ: Double = shadowPlane): Op {
        val a = project(castShadow(from, planeZ)); val b = project(castShadow(to, planeZ))
        val shader = LinearGradient(a.x.toFloat(), a.y.toFloat(), b.x.toFloat() + 1e-3f, b.y.toFloat(), argb(SHADOW, .36), argb(SHADOW, .12), Shader.TileMode.CLAMP)
        return poly2(hull(vertices.map { project(castShadow(it, planeZ)) }), 0xffffff, 1.0, SOFT, shader)
    }
    // Ambient occlusion right where a solid touches the ground.
    fun contact(c: V, r: Double) = poly(ring(c, r + 1, 0.0), SHADOW, .32, CONTACT)
    fun contactPoly(ps: List<V>) = poly(ps, SHADOW, .3, CONTACT)

    private fun sideGradient(side: List<P>, rgb: Int, normalZ: Double): Shader {
        val xs = side.map { it.x }
        val colors = IntArray(9); val stops = FloatArray(9)
        for (i in 0 until 9) {
            val phi = PI * (1 - i / 8.0)
            val n = Cam.RIGHT * cos(phi) + Cam.TOWARD * sin(phi)
            stops[i] = ((1 + cos(phi)) / 2).toFloat()
            colors[i] = argb(tint(rgb, intensity(if (normalZ == 0.0) n else (n + V(0.0, 0.0, normalZ)).norm()) * .93))
        }
        return LinearGradient(xs.min().toFloat(), 0f, xs.max().toFloat() + 1e-3f, 0f, colors, stops, Shader.TileMode.CLAMP)
    }

    // Cylinder: silhouette = hull of both rims, side shaded by the real normal across the visible arc.
    fun cylinder(c: V, r: Double, h: Double, material: Int): List<Op> {
        val top = ring(c, r, h); val bottom = ring(c, r, 0.0)
        val side = hull((top + bottom).map(::project))
        val seen = top.mapIndexed { i, p -> val t = i * 2 * PI / SEGMENTS; v.faces(V(cos(t), sin(t)), p) }
        val start = seen.indices.indexOfFirst { seen[it] && !seen[(it + SEGMENTS - 1) % SEGMENTS] }
        val arc = ArrayList<P>()
        if (start >= 0) for (k in 0 until SEGMENTS) { val i = (start + k) % SEGMENTS; if (!seen[i]) break; arc.add(project(top[i])) }
        val arcPath = pathOf(arc, close = false); val arcPaint = strokePaint(0xffffff, .8, .9)
        return listOf(poly2(side, 0, shader = sideGradient(side, material, 0.0)), poly(top, material), { cv -> cv.drawPath(arcPath, arcPaint) })
    }

    // Frustum (tapered drum) standing at c: bottom radius rb, top radius rt, height h.
    fun frustum(c: V, rb: Double, rt: Double, h: Double, material: Int, rim: Boolean = true): List<Op> {
        val top = ring(c, max(rt, .001), h); val bottom = ring(c, rb, 0.0)
        val side = hull((top + bottom).map(::project))
        val ops = mutableListOf(poly2(side, 0, shader = sideGradient(side, material, (rb - rt) / h)))
        if (rt > 0) ops += if (rim) poly(top, material, stroke = 0xffffff, strokeWidth = .5, strokeAlpha = .5) else poly(top, material)
        return ops
    }

    // Convex polyhedron: draw exactly the faces that turn towards the camera, Lambert-shaded.
    fun solid(faces: List<Face>, matl: Int, stroke: Int): List<Op> {
        val mid = centroid(faces.flatMap { it.ps })
        return faces.mapNotNull { face ->
            var n = face.n ?: ((face.ps[1] - face.ps[0]) cross (face.ps[2] - face.ps[0])).norm()
            if ((n dot (centroid(face.ps) - mid)) < 0) n = -n
            if (v.faces(n, centroid(face.ps))) poly(face.ps, tint(face.matl ?: matl, intensity(n)), stroke = stroke, strokeWidth = .7) else null
        }
    }

    fun clipTo(ps: List<V>, ops: List<Op>): Op {
        val path = pathOf(ps.map(::project))
        return { c -> c.save(); c.clipPath(path); ops.forEach { it(c) }; c.restore() }
    }

    // 1. Stage 1 — the 30-60-90 prism, mirrored: right angle on the left, the 30° tip points right.
    fun prism(p: Prop): PropOut {
        val m = -1.0; val w = 70.0; val h = 70 / sqrt(3.0); val sc = 1.55; val thickness = 15.0; val plinth = 8.0
        val color = 0x5b8cff; val stroke = 0x3f6fd9; val ink = 0xffffff; val center = p.center
        fun world(x: Double, y: Double, z: Double) = center + V(m * (x - w / 2 - 2) * sc, y, z * sc + plinth).rotZ(p.yaw)
        fun tri(y: Double) = listOf(world(0.0, y, 0.0), world(w, y, 0.0), world(w, y, h))
        fun rot(n: V) = n.rotZ(p.yaw)
        val f = tri(thickness / 2); val b = tri(-thickness / 2)
        val s = ArrayList<Op>()
        s += cylinder(center, PRISM_RADIUS, plinth, PLINTH)
        s += clipTo(ring(center, PRISM_RADIUS, plinth), listOf(shadowShape(f + b, world(w, 0.0, 0.0), world(w, 0.0, h), plinth)))
        s += solid(listOf(
            Face(f, rot(V(0.0, 1.0))), Face(b, rot(V(0.0, -1.0))),
            Face(listOf(f[1], b[1], b[2], f[2]), rot(V(m, 0.0))),
            Face(listOf(f[0], f[2], b[2], b[0]), rot(V(-m * h, 0.0, w).norm())),
        ), color, stroke)
        s.add(line3(listOf(f[0], f[1]), 0x3f6f8f, .9, .5))
        val fy = thickness / 2
        if (v.faces(rot(V(0.0, 1.0)), f[0])) {
            // Lettering on the front plane; text always runs left→right even when the solid is mirrored.
            fun on(x: Double, z: Double) = world(x, fy, z)
            val ex = (world(1.0, fy, 0.0) - world(0.0, fy, 0.0)) * m
            val ez = world(0.0, fy, -1.0) - world(0.0, fy, 0.0)
            fun txt(x: Double, z: Double, t: String, size: Double, r: Double = 0.0) =
                framed(on(x, z), ex, ez) { c -> c.rotate(r.toFloat()); label(c, 0.0, size * .35, t, size, ink) }
            fun ln(ps: List<Pair<Double, Double>>) = { c: NCanvas ->
                c.drawPath(pathOf(ps.map { project(on(it.first, it.second)) }, close = false), strokePaint(ink, 1.0).apply { strokeCap = Paint.Cap.BUTT })
            }
            val a0 = atan2(h, w); val mm = 7.0
            s += ln(listOf(w - mm to 0.0, w - mm to mm, w to mm))
            s += ln(List(9) { i -> val t = a0 * i / 8; 12 * cos(t) to 12 * sin(t) })
            s += txt(19.0, 3.2, "30°", 6.5)
            s += txt(w * .6, 3.6, "√3", 7.0)
            s += txt(w - 4.8, h * .45, "1", 7.0)
            s += txt(w / 2 + 5.5 * sin(a0), h / 2 - 5.5 * cos(a0), "2", 7.0, -m * a0 * 180 / PI)
            s += txt(w - 5.5, h - (6.5 * h / w + 7), "60°", 5.5)
        }
        val g = listOf(
            shadowShape(ring(center, PRISM_RADIUS, 0.0) + ring(center, PRISM_RADIUS, plinth), center, center.up(plinth)),
            contact(center, PRISM_RADIUS),
            shadowShape(f + b, world(w, 0.0, 0.0), world(w, 0.0, h), 0.0),
        )
        return PropOut(g, s)
    }

    // Solid 3D lettering: the same drawing stacked from back to front.
    private fun drawSen30(c: NCanvas, rgb: Int, alpha: Double, blur: BlurMaskFilter?, stroke: Pair<Int, Double>?) {
        c.save(); c.scale(1.22f, 1.22f); c.translate(30f, 0f)
        text(c, "sen 30° =", 14.0, -2.0, 20.0, rgb, WORD_FACE, Paint.Align.RIGHT, alpha, blur, stroke)
        text(c, "1", 29.0, -17.5, 13.0, rgb, WORD_FACE, alpha = alpha, blur = blur, stroke = stroke)
        val rect = RectF(21.5f, -15.4f, 36.5f, -13.2f)
        if (stroke != null) c.drawRoundRect(rect, 1.1f, 1.1f, strokePaint(stroke.first, stroke.second, alpha).apply { strokeCap = Paint.Cap.BUTT })
        c.drawRoundRect(rect, 1.1f, 1.1f, fill(rgb, alpha, blur))
        text(c, "2", 29.0, -1.0, 13.0, rgb, WORD_FACE, alpha = alpha, blur = blur, stroke = stroke)
        c.restore()
    }
    private fun drawTan45(c: NCanvas, rgb: Int, alpha: Double, blur: BlurMaskFilter?, stroke: Pair<Int, Double>?) {
        c.save(); c.scale(1.22f, 1.22f)
        text(c, "tg 45° = 1", 0.0, -2.0, 20.0, rgb, WORD_FACE, alpha = alpha, blur = blur, stroke = stroke)
        c.restore()
    }

    fun letters(p: Prop): PropOut {
        val sen = p.kind == Kind.SEN30
        val color = if (sen) 0x3ec6ff else 0xff4f9a; val sideTone = if (sen) 0x1f93cc else 0xcf2f74; val edge = if (sen) 0x1a84b8 else 0xbd2a6a
        val draw: (NCanvas, Int, Double, BlurMaskFilter?, Pair<Int, Double>?) -> Unit = if (sen) ::drawSen30 else ::drawTan45
        val depth = 5.0; val layers = 10
        val ex = Cam.RIGHT.rotZ(p.yaw); val ez = V(0.0, 0.0, -1.0); val back = (-Cam.TOWARD).rotZ(p.yaw)
        fun at(k: Double) = p.center + back * (depth * k)
        val svg = ArrayList<Op>()
        for (k in layers downTo 1) { val tone = if (k == 1) tint(color, .82) else sideTone; svg += framed(at(k.toDouble() / layers), ex, ez) { c -> draw(c, tone, 1.0, null, null) } }
        svg += framed(at(0.0), ex, ez) { c -> draw(c, color, 1.0, null, edge to .45) }
        // Its shadow: the drawing sheared onto the ground along the light; contact flattened at its foot.
        val down = V(LIGHT.x / LIGHT.z, LIGHT.y / LIGHT.z, 0.0)
        fun o(k: Double) = castShadow(at(k), shadowPlane)
        val ground = listOf(0.0, .5, 1.0).map { k -> framed(o(k), ex, down) { c -> draw(c, SHADOW, .16, SOFT, null) } } +
            framed(o(.5), ex, back * .2) { c -> draw(c, SHADOW, .28, SOFT, null) }
        return PropOut(ground, svg)
    }

    // 3. Stage 2 — angle of elevation: a lighthouse of height h seen from a point on the ground at angle θ.
    fun lighthouse(p: Prop): PropOut {
        val c = p.center; val distance = 48.0
        val u = Cam.RIGHT.rotZ(p.yaw).norm(); val g0 = c + u * distance; val toward = -u
        val bands = 5; val bandH = 10.0; val rb = 12.0; val rt = 8.5
        fun r(z: Double) = rb + (rt - rb) * z / (bands * bandH)
        val s = ArrayList<Op>()
        s += frustum(c, 15.0, 13.5, 4.0, 0xe6edf2)
        for (k in 0 until bands) s += frustum(c.up(4 + k * bandH), r(k * bandH), r((k + 1) * bandH), bandH, if (k % 2 == 1) 0xfbf7f2 else 0xff7d6e)
        val z1 = 4 + bands * bandH
        s += frustum(c.up(z1), 12.0, 12.0, 2.4, 0x56748a)
        s += frustum(c.up(z1 + 2.4), 6.2, 6.2, 8.0, 0xffe7a0)
        s += frustum(c.up(z1 + 10.4), 8.8, 0.0, 8.0, 0xff7d6e, false)
        val hh = z1 + 18.4; val top = c.up(hh); val theta = atan2(hh, distance)
        val red = 0xb3473b
        val arc = List(10) { i -> val t = theta * i / 9; g0 + toward * (16 * cos(t)) + V(0.0, 0.0, 16 * sin(t)) }
        s += line3(listOf(g0, c), red, 1.1, dash = floatArrayOf(3f, 3f))
        s += line3(listOf(g0, top), red, 1.5, dash = floatArrayOf(4f, 3f))
        s += line3(arc, red, 1.3)
        val m = project(g0); val rx = Cam.scale * 3; val ry = rx * Cam.se
        s += { cv -> cv.drawOval(RectF((m.x - rx).toFloat(), (m.y - ry).toFloat(), (m.x + rx).toFloat(), (m.y + ry).toFloat()), fill(red)) }
        s += tag(g0 + toward * (24 * cos(theta / 2)) + V(0.0, 0.0, 24 * sin(theta / 2)), "θ", 10.0, red, 4.0)
        s += tag(c.up(hh / 2), "h", 11.0, red, -(rb * Cam.scale + 9))
        s += tag(g0 + toward * (distance / 2), "d", 10.0, red, 0.0, 8.0)
        val gr = listOf(shadowShape(ring(c, 15.0, 0.0) + ring(c, rb, 4.0) + ring(c, rt, z1) + ring(c, .5, hh), c, top), contact(c, 15.0))
        return PropOut(gr, s, lamp = project(c.up(z1 + 6.4)))
    }

    // 4. Stage 2 — a kite on a taut string: string length, angle θ with the ground, height h.
    fun kite(p: Prop): PropOut {
        val u = Cam.RIGHT.rotZ(p.yaw).norm(); val n = Cam.TOWARD.rotZ(p.yaw).norm(); val g0 = p.center
        val theta = atan2(KITE_HEIGHT, KITE_RUN)
        val k0 = g0 + u * KITE_RUN + V(0.0, 0.0, KITE_HEIGHT)
        // The kite leans back 25° into the wind and its two halves fold slightly along the spine.
        val lean = rad(25.0); val vv = V(0.0, 0.0, cos(lean)) + n * -sin(lean)
        fun at(x: Double, y: Double, back: Double = 0.0) = k0 + u * x + vv * y + n * -back
        val top = at(0.0, 20.0); val bottom = at(0.0, -22.0); val left = at(-15.0, 3.0, 4.0); val right = at(15.0, 3.0, 4.0); val mid = at(0.0, 3.0)
        fun panel(ps: List<V>, col: Int): Op {
            var nn = ((ps[1] - ps[0]) cross (ps[2] - ps[0])).norm()
            if ((nn dot Cam.VIEW) < 0) nn = -nn
            return poly(ps, tint(col, min(1.08, intensity(nn) * 1.08)), stroke = 0xffffff, strokeWidth = .5, strokeAlpha = .7)
        }
        val air = arrayListOf(panel(listOf(top, left, mid), 0xffd23f), panel(listOf(left, bottom, mid), 0xa96bff), panel(listOf(top, right, mid), 0xa96bff), panel(listOf(right, bottom, mid), 0xffd23f))
        air += line3(listOf(top, bottom), 0x6a3fc0, .9); air += line3(listOf(left, mid, right), 0x6a3fc0, .9)
        // Soft ribbon tail with three bows.
        val tail = List(14) { i -> at(3.2 * sin(i * .75), -22 - i * 2.3) + n * (-i * .2) }
        air += line3(tail, 0x6a3fc0, .8)
        listOf(4, 8, 12).forEachIndexed { k, i ->
            val q = tail[i]; val col = if (k % 2 == 1) 0x2fd3c0 else 0xff4f9a
            air += poly(listOf(q + u * -4.0 + V(0.0, 0.0, 1.8), q, q + u * -4.0 + V(0.0, 0.0, -1.8)), col)
            air += poly(listOf(q + u * 4.0 + V(0.0, 0.0, 1.8), q, q + u * 4.0 + V(0.0, 0.0, -1.8)), col)
        }
        // String with a slight sag from the spool to the bridle.
        val string = List(16) { i -> val t = i / 15.0; g0 * (1 - t) + mid * t + V(0.0, 0.0, -5 * 4 * t * (1 - t)) }
        val violet = 0x4b3b9a
        val s = arrayListOf(line3(string, 0x3d4a56, .9))
        val foot = g0 + u * KITE_RUN
        s += line3(listOf(mid, foot), violet, 1.0, dash = floatArrayOf(3f, 3f))
        s += line3(listOf(g0, foot), violet, 1.0, dash = floatArrayOf(3f, 3f))
        val arc = List(10) { i -> val t = theta * i / 9; g0 + u * (15 * cos(t)) + V(0.0, 0.0, 15 * sin(t)) }
        s += line3(arc, violet, 1.3)
        s += tag(g0 + u * (23 * cos(theta / 2)) + V(0.0, 0.0, 23 * sin(theta / 2)), "θ", 10.0, violet, 2.0)
        s += tag(foot.up(KITE_HEIGHT * .4), "h", 11.0, violet, -8.0)
        s += frustum(g0, 3.4, 3.4, 3.4, 0x4fa8ff); s += frustum(g0.up(3.4), 1.7, 1.7, 2.0, 0x2f6fd0, false)
        val kiteShadow = poly(listOf(top, right, bottom, left).map { castShadow(it, shadowPlane) }, SHADOW, .18, SOFT)
        return PropOut(listOf(kiteShadow, lineShadow(string, 1.0), lineShadow(tail, .8), contact(g0, 3.5)), s, air, project(mid))
    }

    // 5. Stage 3 — a ladder leaning on a brick wall at 60°.
    fun ladder(p: Prop): PropOut {
        val c = p.center; val n = Cam.RIGHT.rotZ(p.yaw); val a = Cam.TOWARD.rotZ(p.yaw); val t = 13.0; val lw = 50.0; val hh = 52.0
        val box = listOf(-1, 1).flatMap { sx -> listOf(-1, 1).map { sy -> c + a * (sx * lw / 2) + n * (sy * t / 2) } }
        val (p00, p01, p10, p11) = box
        fun up(q: V) = q.up(hh)
        val bricks = intArrayOf(0xc65a45, 0xb84f3c, 0xd0664f, 0xbf5641, 0xc95e48)
        var seed = 3L
        fun rnd(): Double { seed = seed * 16807 % 2147483647; return seed / 2147483647.0 }
        class Side(val q0: V, val dir: V, val len: Double, val nrm: V, val around: Double)
        val sides = listOf(Side(p01, a, lw, n, 0.0), Side(p10, n, t, a, lw), Side(p00, a, lw, -n, 0.0), Side(p00, n, t, -a, 0.0))
        val s = ArrayList<Op>()
        for (f in sides) {
            val q1 = f.q0 + f.dir * f.len; val mid = (f.q0 + q1) * .5 + V(0.0, 0.0, hh / 2)
            if (!v.faces(f.nrm, mid)) continue
            val k = intensity(f.nrm)
            s += poly(listOf(f.q0, q1, up(q1), up(f.q0)), tint(0xe6dccd, k))
            val rows = 10; val rh = hh / rows; val bl = 10.5; val g = .55
            for (r in 0 until rows) {
                val off = ((r % 2) * bl / 2 + f.around) % bl
                var uu = -off
                while (uu < f.len) {
                    val u0 = max(0.0, uu) + g; val u1 = min(f.len, uu + bl) - g
                    if (u1 - u0 >= 1) {
                        val z0 = r * rh + g; val z1 = (r + 1) * rh - g
                        fun at(x: Double, z: Double) = f.q0 + f.dir * x + V(0.0, 0.0, z)
                        s += poly(listOf(at(u0, z0), at(u1, z0), at(u1, z1), at(u0, z1)), tint(bricks[floor(rnd() * bricks.size).toInt()], k))
                    }
                    uu += bl
                }
            }
        }
        s += poly(listOf(up(p00), up(p10), up(p11), up(p01)), 0xd9d3c8, stroke = 0xc4bdb0, strokeWidth = .6)
        // Ladder: two square rails and rungs, solid 3D beams, drawn far → near.
        val run = hh / tan(rad(60.0)); val wallFace = c + n * (t / 2); val foot = wallFace + n * run
        fun beam(p0: V, p1: V, w: Double, col: Int): Pair<Double, List<Op>> {
            val d = (p1 - p0).norm(); val sd = (if (a.x * d.x + a.y * d.y > .9) n else a).norm(); val ot = (d cross sd).norm()
            fun corner(q: V, i: Int, j: Int) = q + sd * (i * w / 2) + ot * (j * w / 2)
            val r4 = listOf(-1 to -1, 1 to -1, 1 to 1, -1 to 1)
            val faces = r4.indices.map { i -> val q = r4[i]; val r = r4[(i + 1) % 4]; Face(listOf(corner(p0, q.first, q.second), corner(p0, r.first, r.second), corner(p1, r.first, r.second), corner(p1, q.first, q.second))) }
            return v.depth((p0 + p1) * .5) to solid(faces, col, tint(col, .75))
        }
        fun rail(k: Double) = listOf(foot + a * k, wallFace.up(hh - 3) + a * k)
        val rails = listOf(rail(-6.5), rail(6.5))
        val parts = rails.map { beam(it[0], it[1], 2.4, 0xffd23f) }.toMutableList()
        for (i in 1..6) {
            val tt = i / 7.0
            val q0 = rails[0][0] + (rails[0][1] - rails[0][0]) * tt; val q1 = rails[1][0] + (rails[1][1] - rails[1][0]) * tt
            parts += beam(q0, q1, 1.6, 0xf0bf1a)
        }
        parts.sortBy { it.first }; parts.forEach { s += it.second }
        val arc = List(10) { i -> val tt = rad(60.0) * i / 9; foot + n * (-12 * cos(tt)) + V(0.0, 0.0, 12 * sin(tt)) }
        s += line3(arc, 0x7a2f22, 1.3); s += tag(foot + n * -7.0, "60°", 8.0, 0x7a2f22, 2.0, 11.0)
        val gr = listOf(shadowShape(box + box.map(::up), c, up(c)), contactPoly(listOf(p00, p01, p11, p10))) + rails.map { lineShadow(it, 2.6) }
        return PropOut(gr, s)
    }

    // Stage finish: a trophy on a small plinth, gold once every node of the stage is done.
    fun trophy(p: Prop): PropOut {
        val c = p.center; val done = state.stageDone(p.stage)
        val gold = if (done) 0xffc83d else 0xd3dbe2; val deep = if (done) 0xd99a00 else 0xaab6c0; val base = if (done) 0x3f5a78 else 0x8b9aa8; val sc = 1.4
        val s = ArrayList<Op>()
        s += frustum(c, 9 * sc, 8 * sc, 4 * sc, base); s += frustum(c.up(4 * sc), 6.5 * sc, 6 * sc, 2 * sc, base)
        s += frustum(c.up(6 * sc), 2.4 * sc, 2 * sc, 7 * sc, gold); s += frustum(c.up(13 * sc), 4 * sc, 4 * sc, 2 * sc, deep)
        val cup = c.up(15 * sc)
        fun handle(side: Int) = List(9) { i -> val t = i / 8.0; cup + Cam.RIGHT * (side * (8 + 5.5 * sin(PI * t)) * sc) + V(0.0, 0.0, (12 - 8 * t) * sc) }
        s += line3(handle(-1), deep, 2.4 * sc); s += line3(handle(1), deep, 2.4 * sc)
        s += frustum(cup, 5 * sc, 11 * sc, 14 * sc, gold, false)
        s += poly(ring(cup, 11 * sc, 14 * sc), tint(gold, 1.05)); s += poly(ring(cup, 9.4 * sc, 14 * sc), deep)
        val face = cup + Cam.TOWARD * (9.8 * sc) + V(0.0, 0.0, 9.5 * sc)
        val starFill = fill(if (done) 0xfff6c8 else 0xeef2f5)
        s += framed(face, Cam.RIGHT * (.28 * sc), V(0.0, 0.0, -.28 * sc)) { cv -> cv.drawPath(STAR_PATH, starFill) }
        return PropOut(listOf(shadowShape(ring(c, 9 * sc, 0.0) + ring(cup, 11 * sc, 14 * sc), c, cup.up(14 * sc)), contact(c, 9 * sc)), s)
    }

    fun kind(p: Prop): PropOut = when (p.kind) {
        Kind.PRISM -> prism(p)
        Kind.SEN30, Kind.TAN45 -> letters(p)
        Kind.LIGHTHOUSE -> lighthouse(p)
        Kind.KITE -> kite(p)
        Kind.LADDER -> ladder(p)
        Kind.TROPHY -> trophy(p)
    }

    // Every sculpture stands on a plinth; its shadows then fall on the plinth top (clipped), overshoot onto the ground.
    fun drawProp(p: Prop): PropOut {
        val pl = p.plinth ?: return kind(p)
        val h = pl.h
        val base: Triple<List<Op>, List<V>, List<Op>> = if (pl.round) {
            val c = p.center + Cam.RIGHT.rotZ(p.yaw).norm() * pl.shift
            Triple(cylinder(c, pl.r, h, PLINTH), ring(c, pl.r, h), listOf(shadowShape(ring(c, pl.r, 0.0) + ring(c, pl.r, h), c, c.up(h), 0.0), contact(c, pl.r)))
        } else {
            val (c, l, d) = slabCenter(p)
            val g = slabGeometry(c, Cam.RIGHT.rotZ(p.yaw), Cam.TOWARD.rotZ(p.yaw), l, d, h)
            Triple(solid(g.faces, PLINTH, 0xdcd6c9), g.top, listOf(shadowShape(g.bottom + g.top, c, c.up(h), 0.0), contactPoly(g.bottom)))
        }
        val lifted = p.lifted(h)
        shadowPlane = 0.0; val onGround = kind(lifted)
        shadowPlane = h; val onTop = kind(lifted); shadowPlane = 0.0
        return PropOut(base.third + onGround.ground, base.first + clipTo(base.second, onTop.ground) + onTop.svg, onTop.air, onTop.pivot, onTop.lamp)
    }

    // Stage transition: nodes and sculptures of a stage pop up from their base as it scrolls in (staggered along the path).
    fun pop(stage: Int, k: Double): Pair<Double, Double> {
        if (calm) return 1.0 to 1.0
        val vis = 1 - min(1.0, abs(v.scroll - stage * SCREEN) / SCREEN)
        val t = ((vis - .3 - k * .025) / .45).coerceIn(0.0, 1.0)
        val c1 = 1.25; val c3 = c1 + 1
        val s = if (t >= 1) 1.0 else 1 + c3 * (t - 1) * (t - 1) * (t - 1) + c1 * (t - 1) * (t - 1)
        return s to min(1.0, t * 1.8)
    }
    fun popWrap(ops: List<Op>, base: P, p: Pair<Double, Double>): Op? {
        val (s, o) = p
        if (s >= 1 && o >= 1) return { c -> ops.forEach { it(c) } }
        if (o <= .01) return null
        return { c ->
            c.save()
            c.translate(base.x.toFloat(), base.y.toFloat()); c.scale(max(.001, s).toFloat(), max(.001, s).toFloat()); c.translate(-base.x.toFloat(), -base.y.toFloat())
            c.saveLayerAlpha(null, jsRound(o * 255))
            ops.forEach { it(c) }
            c.restore(); c.restore()
        }
    }

    fun build(yPad: Double): Frame {
        fun onScreen(p: V): Boolean {
            if (v.toCamera(p).z > Cam.DIST * .7) return false
            val q = project(p); return q.y > -160 - yPad && q.y < 1020 + yPad
        }
        val ground = ArrayList<Op>(); val objects = ArrayList<Pair<Double, Op>>()
        var air: Op? = null; var pivot: P? = null; var lamp: P? = null; var lampO = 0.0
        val current = state.current
        // Path dots on the ground: light up to the current node; each stage's path ends at its trophy.
        class Seg(val a: V, val b: V, val ra: Double, val rb: Double, val lit: Boolean, val stage: Int, val k: Double)
        val activeNodes = NODES.indices.filter { !NODES[it].book || state.lessonAt[it] != null }
        val segs = activeNodes.zipWithNext().filter { NODES[it.first].stage == NODES[it.second].stage }
            .map { (a, b) -> Seg(LESSONS[a], LESSONS[b], radiusOf(a), radiusOf(b), a < current, NODES[b].stage, (b - 1 - NODES.indexOfFirst { n -> n.stage == NODES[b].stage }).toDouble()) } +
            STAGES.indices.map { s ->
                val last = NODES.indices.last { NODES[it].stage == s }; val t = PROPS.first { it.kind == Kind.TROPHY && it.stage == s }
                Seg(LESSONS[last], t.center, radiusOf(last), t.plinth!!.r, last < current, s, 8.0)
            }
        for (g in segs) {
            if (!onScreen(g.a) && !onScreen(g.b)) continue
            val p = pop(g.stage, g.k); if (p.second < .5) continue
            val d = g.b - g.a; val len = hypot(d.x, d.y); val u = d * (1 / len)
            val from = g.ra + 7; val to = len - g.rb - 10; val n = max(1, jsRound((to - from) / 9))
            val color = if (g.lit) 0xfff1a8 else 0xf7fbf2
            for (k in 0..n) ground += poly(ring(g.a + u * (from + (to - from) * k / n), 2.1, 0.0).filterIndexed { j, _ -> j % 7 == 0 }, color)
        }
        LESSONS.forEachIndexed { i, c ->
            if (NODES[i].book && state.lessonAt[i] == null) return@forEachIndexed
            if (!onScreen(c)) return@forEachIndexed
            val node = NODES[i]; val r = radiusOf(i); val th = if (node.lesson) TILE_HEIGHT else if (node.book) 7.0 else 6.0; val top = c.up(th)
            val p = pop(node.stage, node.k.toDouble()); val base = project(c)
            popWrap(listOf(shadowShape(ring(c, r, 0.0) + ring(c, r, th), c, top), contact(c, r)), base, p)?.let { ground += it }
            val material = if (i < state.done) 0xffd84d else if (i == current) 0x8fd8ff else 0xf2f5f7
            val locked = i > current
            // A book introduces the concept; numbered lessons and reviews keep their own marks.
            val mark = framed(top, Cam.RIGHT * (1 / Cam.scale), Cam.TOWARD * (1 / Cam.scale)) { cv ->
                if (node.lesson) label(cv, 0.0, 8.0, lessonNumber(i).toString(), 24.0, 0x245271, if (locked) .72 else 1.0)
                else if (node.book) cv.drawPath(BOOK_PATH, strokePaint(if (locked) 0x8aa3b5 else 0x245271, 2.5))
                else cv.drawPath(REPEAT_PATH, strokePaint(if (locked) 0x8aa3b5 else 0x245271, 2.6))
            }
            popWrap(cylinder(c, r, th, material) + mark, base, p)?.let { objects += v.depth(c) to it }
        }
        for (pp in PROPS) {
            if (!onScreen(pp.center)) continue
            val r = drawProp(pp); val p = pop(pp.stage, pp.k); val base = project(pp.center)
            popWrap(r.ground, base, p)?.let { ground += it }
            popWrap(r.svg, base, p)?.let { objects += v.depth(pp.center) to it }
            if (r.air != null) { air = popWrap(r.air, base, p); pivot = r.pivot }
            if (r.lamp != null) { lamp = r.lamp; lampO = p.second * if (p.first > .95) 1.0 else 0.0 }
        }
        objects.sortBy { it.first }
        return Frame(ground, objects.map { it.second }, air, pivot, lamp, lampO)
    }
}

// ---------- Lawn: painted once in map coordinates, laid on the ground plane with the exact perspective homography ----------
private class LawnTile(val bitmap: Bitmap, val y0: Double)
private class Lawn(val x0: Double, val res: Double, val tiles: List<LawnTile>)
private val lawnLock = Any()
private var cachedLawn: Pair<Double, SoftReference<Lawn>>? = null
private fun reusableLawn(yPad: Double): Lawn = synchronized(lawnLock) {
    cachedLawn?.takeIf { it.first == yPad }?.second?.get()
        ?: paintLawn(yPad).also { cachedLawn = yPad to SoftReference(it) }
}

// Homography map-units → scene-units for the current camera.
private fun groundMatrix(v: View3): Matrix {
    val sq = listOf(P(0.0, 0.0), P(400.0, 0.0), P(400.0, 400.0), P(0.0, 400.0))
    val src = FloatArray(8); val dst = FloatArray(8)
    sq.forEachIndexed { i, p ->
        val q = v.project(Cam.mapToWorld(p.x, p.y))
        src[2 * i] = p.x.toFloat(); src[2 * i + 1] = p.y.toFloat(); dst[2 * i] = q.x.toFloat(); dst[2 * i + 1] = q.y.toFloat()
    }
    return Matrix().apply { setPolyToPoly(src, 0, dst, 0, 4) }
}

private fun segDist(p: P, a: P, b: P): Double {
    val dx = b.x - a.x; val dy = b.y - a.y
    val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
    return hypot(p.x - a.x - dx * t, p.y - a.y - dy * t)
}
private fun inPoly(p: P, ps: List<P>): Boolean {
    var s = 0.0
    for (i in ps.indices) {
        val a = ps[i]; val b = ps[(i + 1) % ps.size]
        val c = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
        if (c != 0.0) { if (s == 0.0) s = Math.signum(c) else if (Math.signum(c) != s) return false }
    }
    return true
}
private fun polyDist(p: P, ps: List<P>) = if (inPoly(p, ps)) 0.0 else ps.indices.minOf { segDist(p, ps[it], ps[(it + 1) % ps.size]) }

private fun paintLawn(yPad: Double): Lawn {
    // Area the ground must cover: every screen corner, at the first, middle and last scroll position.
    val xs = ArrayList<Double>(); val ys = ArrayList<Double>()
    val maxScroll = MAP_HEIGHT - SCREEN
    for (s in listOf(0.0, maxScroll / 2, maxScroll)) {
        val inv = Matrix().also { groundMatrix(View3(s)).invert(it) }
        val pts = floatArrayOf(0f, -yPad.toFloat(), 390f, -yPad.toFloat(), 0f, (SCREEN + yPad).toFloat(), 390f, (SCREEN + yPad).toFloat())
        inv.mapPoints(pts)
        for (i in 0 until 4) { xs += pts[2 * i].toDouble(); ys += pts[2 * i + 1].toDouble() }
    }
    val x0 = floor(xs.min()) - 24; val y0 = floor(ys.min()) - 24
    val w = ceil(xs.max()) + 24 - x0; val h = ceil(ys.max()) + 24 - y0
    val res = min(3.8, sqrt(7e6 / (w * h)))

    var seed = 5L
    fun rnd(): Double { seed = seed * 16807 % 2147483647; return seed / 2147483647.0 }
    val se = Cam.se; val vert = Cam.scale * Cam.ce
    val lessonsMap = LESSONS.mapIndexed { i, c -> Cam.mapToScreen0(c) to radiusOf(i) * Cam.scale }
    val pathPts = LESSONS.map(Cam::mapToScreen0)
    fun nearPath(x: Double, y: Double, m: Double) = (1 until pathPts.size).any { NODES[it - 1].stage == NODES[it].stage && segDist(P(x, y), pathPts[it - 1], pathPts[it]) < m }
    fun inLesson(x: Double, y: Double, k: Double) = lessonsMap.any { (p, r) -> hypot((x - p.x) / r, (y - p.y) / (r * se)) < k }
    fun hidden(x: Double, y: Double) = inLesson(x, y, 1.1) || nearPath(x, y, 10.0)
    val footprints = PROPS.map { p -> plinthBottom(p).map(Cam::mapToScreen0) to p.tall }

    // Drawing primitives in map coordinates, tagged with their y so each tile paints only what touches it.
    val prims = ArrayList<Pair<Double, (NCanvas) -> Unit>>()
    // Stylised lawn: one clean green, a few soft patches, sparse simple tufts.
    fun patch(x: Double, y: Double, r: Double, rgb: Int) {
        val paint = fill(0, shader = RadialGradient(0f, 0f, r.toFloat(), intArrayOf(argb(rgb), argb(rgb), argb(rgb, 0.0)), floatArrayOf(0f, .72f, 1f), Shader.TileMode.CLAMP))
        prims += y to { c -> c.save(); c.translate(x.toFloat(), y.toFloat()); c.scale(1f, se.toFloat()); c.drawCircle(0f, 0f, r.toFloat(), paint); c.restore() }
    }
    for (i in 0 until (w * h / 5200).toInt()) {
        val x = x0 + rnd() * w; val y = y0 + rnd() * h; val col = if (rnd() < .5) 0x86d261 else 0x72bf4f
        repeat(3) { patch(x + (rnd() - .5) * 40, y + (rnd() - .5) * 22, 16 + rnd() * 26, col) }
    }
    // Tufts vary: 1–6 blades, own fan angle, height, width and a shade picked per tuft; some grow in small groups.
    val shades = listOf(intArrayOf(0x4a9a31, 0x5fb142, 0x98dc72), intArrayOf(0x529f36, 0x6bbb4a, 0xa8e582), intArrayOf(0x43912c, 0x58a93c, 0x8ed468), intArrayOf(0x5aa83c, 0x74c353, 0xb3ea8c))
    val shadePaints = shades.map { colors -> colors.map { fill(it) }.toTypedArray() }
    class Blade(val off: Double, val dx: Double, val h: Double, val w: Double, val front: Boolean)
    class Tuft(val x: Double, val y: Double, val s: Double, val flip: Double, val shade: Array<Paint>, val bl: List<Blade>)
    val tufts = ArrayList<Tuft>()
    fun addTuft(x: Double, y: Double, scale: Double) {
        if (hidden(x, y)) return
        val n = 1 + floor(rnd() * rnd() * 6).toInt() + if (rnd() < .5) 1 else 0
        val spread = .5 + rnd() * 1.1
        val bl = List(n) { k ->
            val u = if (n == 1) 0.0 else k.toDouble() / (n - 1) - .5
            Blade(u * (1.6 + n * .5) + (rnd() - .5) * .8, u * spread * 5 + (rnd() - .5) * 1.8, (3.2 + rnd() * 3.2) * (1 - .3 * abs(u)), 1.15 + rnd() * .75, rnd() < .45)
        }
        tufts += Tuft(x, y, scale * vert, if (rnd() < .5) -1.0 else 1.0, shadePaints[floor(rnd() * shades.size).toInt()], bl)
    }
    for (i in 0 until (w * h / 340).roundToInt()) {
        val x = x0 + rnd() * w; val y = y0 + rnd() * h; val sc = .95 + rnd() * .85
        addTuft(x, y, sc)
        if (rnd() < .4) { var k = 1 + floor(rnd() * 3).toInt(); while (k > 0) { addTuft(x + (rnd() - .5) * 14, y + (rnd() - .5) * 6, sc * (.6 + rnd() * .5)); k-- } }
    }
    tufts.sortBy { it.y }
    val bladePath = Path()
    fun blade(c: NCanvas, paint: Paint, x: Double, y: Double, dx: Double, h: Double, w: Double) {
        val p = bladePath
        p.rewind()
        p.moveTo((x - w).toFloat(), y.toFloat())
        p.quadTo((x - w * .2 + dx * .2).toFloat(), (y - h * .6).toFloat(), (x + dx).toFloat(), (y - h).toFloat())
        p.quadTo((x + w * .6 + dx * .3).toFloat(), (y - h * .5).toFloat(), (x + w).toFloat(), y.toFloat())
        p.close(); c.drawPath(p, paint)
    }
    val tuftShadow = fill(0x285a14, .16)
    for (t in tufts) prims += t.y to { c ->
        val s = t.s; val f = t.flip; val dark = t.shade[0]; val mid = t.shade[1]; val light = t.shade[2]
        c.drawOval(RectF((t.x + 1 - (2 + t.bl.size * 1.2) * s).toFloat(), (t.y + .4 - 1.4 * s).toFloat(), (t.x + 1 + (2 + t.bl.size * 1.2) * s).toFloat(), (t.y + .4 + 1.4 * s).toFloat()), tuftShadow)
        for (b in t.bl) if (!b.front) blade(c, dark, t.x + b.off * s * f, t.y, b.dx * s * f, b.h * s, b.w * s * .85)
        for (b in t.bl) if (b.front) {
            blade(c, mid, t.x + b.off * s * f, t.y, b.dx * s * f, b.h * s, b.w * s)
            blade(c, light, t.x + b.off * s * f + .2 * s, t.y - b.h * s * .3, b.dx * s * f * .8, b.h * s * .7, b.w * s * .3)
        }
        if (t.bl.none { it.front }) { val b = t.bl[0]; blade(c, light, t.x + b.off * s * f, t.y - b.h * s * .35, b.dx * s * f * .7, b.h * s * .6, b.w * s * .25) }
    }
    // Flowers in small clusters, kept away from the path and from every sculpture (and the ground it hides).
    class FlowerKind(val petal: Paint, val core: Paint, val n: Int, val r: Double)
    val kinds = listOf(FlowerKind(fill(0xffffff), fill(0xffd23f), 6, 2.0), FlowerKind(fill(0xffd84d), fill(0xffffff), 5, 1.9), FlowerKind(fill(0xff8cc0), fill(0xfff2a8), 5, 1.9), FlowerKind(fill(0xa98bff), fill(0xfff2a8), 5, 1.9))
    fun blocked(x: Double, y: Double): Boolean {
        if (nearPath(x, y, 20.0) || inLesson(x, y, 1.25)) return true
        return footprints.any { (ps, tall) -> var t = 0.0; var hit = false; while (t <= tall && !hit) { hit = polyDist(P(x, y + t), ps) < 14; t += 8 }; hit }
    }
    class Flower(val x: Double, val y: Double, val k: FlowerKind, val h: Double, val s: Double)
    val fl = ArrayList<Flower>()
    for (k in 0 until (w * h / 14000).toInt()) {
        val cx = x0 + rnd() * w; val cy = y0 + rnd() * h; val kind = kinds[floor(rnd() * kinds.size).toInt()]; val n = 3 + floor(rnd() * 6).toInt()
        repeat(n) { val x = cx + (rnd() * 2 - 1) * 16; val y = cy + (rnd() * 2 - 1) * 11; if (!blocked(x, y)) fl += Flower(x, y, kind, (5 + rnd() * 4) * vert, .85 + rnd() * .4) }
    }
    fl.sortBy { it.y }
    val stem = strokePaint(0x4f9f35, .8).apply { strokeCap = Paint.Cap.BUTT }
    val flowerShadow = fill(0x285a14, .18)
    val flowerPath = Path()
    for (f in fl) prims += f.y to { c ->
        val tx = f.x; val ty = f.y - f.h; val r = f.k.r * f.s
        flowerPath.rewind(); flowerPath.moveTo(f.x.toFloat(), f.y.toFloat()); flowerPath.quadTo((f.x + .8).toFloat(), (f.y - f.h * .5).toFloat(), tx.toFloat(), ty.toFloat())
        c.drawPath(flowerPath, stem)
        c.drawOval(RectF((f.x + 1 - 2.2).toFloat(), (f.y + .3 - .8).toFloat(), (f.x + 1 + 2.2).toFloat(), (f.y + .3 + .8).toFloat()), flowerShadow)
        for (i in 0 until f.k.n) { val a = i * 2 * PI / f.k.n; c.drawCircle((tx + cos(a) * r * .9).toFloat(), (ty + sin(a) * r * .7).toFloat(), (r * .62).toFloat(), f.k.petal) }
        c.drawCircle(tx.toFloat(), ty.toFloat(), (r * .62).toFloat(), f.k.core)
    }

    // Tiles keep every bitmap well inside the GPU texture limit.
    val tileH = 800.0
    val bitmapW = ceil(w * res).toInt()
    val tiles = generateSequence(y0) { it + tileH }.takeWhile { it < y0 + h }.map { ty ->
        val th = min(tileH, y0 + h - ty)
        val bmp = Bitmap.createBitmap(bitmapW, ceil(th * res).toInt() + 1, Bitmap.Config.ARGB_8888)
        val c = NCanvas(bmp)
        c.scale(res.toFloat(), res.toFloat()); c.translate(-x0.toFloat(), -ty.toFloat())
        c.drawRect(x0.toFloat(), ty.toFloat(), (x0 + w).toFloat(), (ty + th + 1).toFloat(), fill(0x7cc957))
        for ((y, draw) in prims) if (y > ty - 60 && y < ty + th + 60) draw(c)
        LawnTile(bmp, ty)
    }.toList()
    return Lawn(x0, res, tiles)
}

private val LAWN_PAINT = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
private fun drawLawn(c: NCanvas, lawn: Lawn, v: View3) {
    val ground = groundMatrix(v)
    for (t in lawn.tiles) {
        val m = Matrix(ground)
        m.preTranslate(lawn.x0.toFloat(), t.y0.toFloat())
        m.preScale((1 / lawn.res).toFloat(), (1 / lawn.res).toFloat())
        c.drawBitmap(t.bitmap, m, LAWN_PAINT)
    }
}

// ---------- Overlays: cloud shadows, the swaying kite and the blinking lighthouse lamp ----------
private class Cloud(val top: Double, val w: Double, val h: Double, val duration: Double, val delay: Double)
private val CLOUDS = listOf(Cloud(.12, .9, .26, 90.0, 10.0), Cloud(.48, 1.2, .30, 120.0, 70.0), Cloud(.76, .7, .26, 100.0, 40.0))
private val CLOUD_SHADER = RadialGradient(0f, 0f, 1f, argb(0x183e12, .13), argb(0x183e12, 0.0), Shader.TileMode.CLAMP)
private val GLOW_SHADER = RadialGradient(0f, 0f, 17.55f, intArrayOf(NColor.argb(242, 255, 236, 150), NColor.argb(89, 255, 220, 110), NColor.argb(0, 255, 220, 110)), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
private val CLOUD_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = CLOUD_SHADER }
private val GLOW_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = GLOW_SHADER }

private fun DrawScope.drawOverlays(frame: Frame, seconds: Double, calm: Boolean, unit: Float, yPad: Double) = drawIntoCanvas { cv ->
    val c = cv.nativeCanvas
    for (cl in CLOUDS) {
        val cw = cl.w * size.width; val ch = cl.h * size.height
        val progress = ((seconds + cl.delay) / cl.duration) % 1.0
        val tx = if (calm) 0.0 else (-1.1 + 2.4 * progress) * cw
        c.save(); c.translate((tx + cw / 2).toFloat(), (cl.top * size.height + ch / 2).toFloat()); c.scale((cw / 2).toFloat(), (ch / 2).toFloat())
        c.drawCircle(0f, 0f, 1f, CLOUD_PAINT); c.restore()
    }
    c.save(); c.scale(unit, unit); c.translate(0f, yPad.toFloat())
    val air = frame.air; val pivot = frame.pivot
    if (air != null && pivot != null) {
        val phase = (seconds / 3.6) % 2.0; val tri = if (phase < 1) phase else 2 - phase
        val angle = if (calm) 0.0 else -3.5 + 6.5 * (1 - cos(PI * tri)) / 2
        c.save(); c.rotate(angle.toFloat(), pivot.x.toFloat(), pivot.y.toFloat()); air(c); c.restore()
    }
    val lamp = frame.lamp
    if (lamp != null && frame.lampOpacity > 0) {
        val blink = if (calm) 1.0 else .15 + .85 * (1 - cos(2 * PI * ((seconds % 2.8) / 2.8))) / 2
        c.save(); c.translate(lamp.x.toFloat(), lamp.y.toFloat())
        GLOW_PAINT.alpha = jsRound(frame.lampOpacity * 255); c.drawCircle(0f, 0f, 17.55f, GLOW_PAINT)
        GLOW_PAINT.alpha = jsRound(frame.lampOpacity * blink * 255); c.drawCircle(0f, 0f, 17.55f, GLOW_PAINT)
        c.restore()
    }
    c.restore()
}

// ---------- Screen ----------
// @spec spec://modules/learning/FEAT-010-learning-demo#course-map
@Composable
internal fun CourseMap3DScreen(
    course: Course,
    progress: ProgressSnapshot,
    onOpen: (Lesson) -> Unit,
    onOpenBook: (Lesson) -> Unit,
    unlockAll: Boolean = false,
    bottomBar: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val calm = remember(context) {
        val lowRam = (context.getSystemService(ActivityManager::class.java))?.isLowRamDevice == true
        lowRam || Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val state = remember(course, progress) { mapProgress(course, progress) }
    val pager = rememberPagerState(initialPage = NODES.getOrNull(state.current)?.stage ?: STAGES.lastIndex) { STAGES.size }
    var nanos by remember { mutableLongStateOf(0L) }
    if (!calm) LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { nanos = it - start }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF7CC957))) {
        val unit = constraints.maxWidth / 390f
        val yPad = (constraints.maxHeight / unit - SCREEN) / 2
        val scroll = { (pager.currentPage + pager.currentPageOffsetFraction) * SCREEN }
        val frame by remember(state, yPad) { derivedStateOf { Builder(View3(scroll()), calm, state).build(abs(yPad)) } }
        val lawn by produceState<Lawn?>(null, yPad) { value = withContext(Dispatchers.Default) { reusableLawn(yPad) } }
        // Separate cached layers: the scene repaints only when the camera moves, the overlay on every animation frame.
        Canvas(Modifier.fillMaxSize().graphicsLayer()) {
            drawIntoCanvas { cv ->
                val c = cv.nativeCanvas
                c.save(); c.scale(unit, unit); c.translate(0f, yPad.toFloat())
                lawn?.let { drawLawn(c, it, View3(scroll())) }
                val f = frame
                f.ground.forEach { it(c) }; f.objects.forEach { it(c) }
                c.restore()
            }
        }
        Canvas(Modifier.fillMaxSize().graphicsLayer()) { drawOverlays(frame, nanos / 1e9, calm, unit, yPad) }
        VerticalPager(pager, Modifier.fillMaxSize()) { page -> StageTargets(page, state, unit, yPad, unlockAll, onOpen, onOpenBook) }
        val stage = pager.currentPage
        val (done, total) = state.stageCount(stage)
        StageCard(progress.xp, streakDays = 0, stage = stage, done = done, total = total, Modifier.align(Alignment.TopCenter).statusBarsPadding())
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) { bottomBar() }
    }
}

// Tap targets sit exactly on the platforms of the stage at rest.
@Composable
private fun StageTargets(page: Int, state: MapProgress, unit: Float, yPad: Double, unlockAll: Boolean, onOpen: (Lesson) -> Unit, onOpenBook: (Lesson) -> Unit) {
    val density = LocalDensity.current
    val view = remember(page) { View3(page * SCREEN) }
    Box(Modifier.fillMaxSize()) {
        NODES.indices.filter { NODES[it].stage == page && state.lessonAt[it] != null }.forEach { i ->
            val lesson = state.lessonAt[i] ?: return@forEach
            val height = if (NODES[i].lesson) TILE_HEIGHT else if (NODES[i].book) 7.0 else 6.0
            val center = view.project(LESSONS[i].up(height))
            val w = radiusOf(i) * Cam.scale * 2; val h = w * Cam.se + height * 2
            val open = unlockAll || i <= state.current
            val status = when { i < state.done -> "Completada"; i == state.current -> "Actual"; else -> "Cerrada" }
            val title = (when { NODES[i].book -> "Concepto"; NODES[i].lesson -> "Lección ${lessonNumber(i)}"; else -> "Repaso" }) + ": ${lesson.title}"
            val action = if (NODES[i].book) onOpenBook else onOpen
            with(density) {
                Box(
                    Modifier
                        .offset { IntOffset(((center.x - w / 2) * unit).roundToInt(), ((center.y + yPad - h / 2) * unit).roundToInt()) }
                        .size((w * unit).toFloat().toDp(), (h * unit).toFloat().toDp())
                        .clip(CircleShape)
                        .clickable(enabled = open) { action(lesson) }
                        .clearAndSetSemantics {
                            contentDescription = title
                            stateDescription = status
                            role = Role.Button
                            if (open) onClick(label = title) { action(lesson); true }
                        },
                )
            }
        }
    }
}

private fun svgPath(d: String) = PathParser().parsePathString(d).toPath()

@Composable
internal fun SvgIcon(paths: List<String>, viewBox: Float, color: Color, strokeWidth: Float, modifier: Modifier, filled: Boolean = false) {
    val parsed = remember(paths) { paths.map(::svgPath) }
    Canvas(modifier) {
        scale(size.width / viewBox, size.height / viewBox, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            parsed.forEach {
                if (filled) drawPath(it, color) else drawPath(it, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

private val TOPIC_BLUE = Color(0xFF2775BF)
private val CARD_INK = Color(0xFF244B68)

@Composable
private fun StageCard(xp: Int, streakDays: Int, stage: Int, done: Int, total: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = Color(0x16315C7D), spotColor = Color(0x30315C7D))
            .background(Color.White, shape)
            .padding(start = 16.dp, end = 16.dp, top = 9.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                SvgIcon(
                    listOf("M5 25H27V7Z"), 32f, TOPIC_BLUE, 2f,
                    Modifier.size(39.dp).clearAndSetSemantics { contentDescription = "Trigonometría" },
                )
                SvgIcon(listOf("M22 25v-5h5"), 32f, TOPIC_BLUE, 1.5f, Modifier.size(39.dp))
            }
            Text(
                buildAnnotatedString {
                    append("$xp ")
                    withStyle(SpanStyle(fontSize = 12.sp, color = Color(0xFF61798A), fontWeight = FontWeight.SemiBold)) { append("XP") }
                },
                color = CARD_INK, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            )
            val active = streakDays > 0
            Row(
                Modifier.weight(1f).clearAndSetSemantics { contentDescription = "Serie de $streakDays días; ${if (active) "experiencia ganada hoy" else "sin experiencia hoy"}" },
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$streakDays", color = CARD_INK, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.width(19.dp).height(24.dp)) {
                    SvgIcon(listOf("M18 1c3 10-6 12-3 20 4-1 6-5 6-9 9 9 11 15 8 21-5 10-21 9-25-1-4-10 2-15 6-20-1 7 1 9 3 10C10 12 19 10 18 1Z"), 32f,
                        if (active) Color(0xFFF7A143) else Color(0xFF9AAAB6), 0f, Modifier.fillMaxSize().padding(0.dp), filled = true)
                    SvgIcon(listOf("M17 22c1 5-4 7-3 11 1 4 7 3 8 0 1-4-2-7-5-11Z"), 32f,
                        if (active) Color(0xFFFFE9B4) else Color(0xFFCBD5DD), 0f, Modifier.fillMaxSize(), filled = true)
                }
            }
        }
        Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Etapa ${stage + 1}", color = Color(0xFF4B6B82), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            val fraction by androidx.compose.animation.core.animateFloatAsState(if (total == 0) 0f else done.toFloat() / total, label = "stage")
            Box(Modifier.weight(1f).padding(horizontal = 9.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE6EEF4))) {
                Box(Modifier.fillMaxWidth(fraction).height(7.dp).clip(RoundedCornerShape(4.dp))
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFFFFC83D), Color(0xFFFFD84D)))))
            }
            Text("$done/$total", color = Color(0xFF7890A2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

internal val LocalAvatarLook = compositionLocalOf { AvatarLook() }

// Bottom dock shared by the map, Tutor and profile.
@Composable
internal fun CourseBottomBar(selectedTab: String, onMap: () -> Unit, onTutor: () -> Unit, onProfile: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    val look = LocalAvatarLook.current
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp).fillMaxWidth().height(84.dp)
            .shadow(12.dp, shape, ambientColor = Color(0x47282814), spotColor = Color(0x47282814))
            .background(Color.White, shape).border(1.dp, Color(0xFFE3E8EF), shape).padding(horizontal=8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockButton("Tape", selectedTab == "map", onMap, listOf("M12 6.8C10 5.2 7 4.6 3.5 5v13.2c3.5-.4 6.5.2 8.5 1.8 2-1.6 5-2.2 8.5-1.8V5C17 4.6 14 5.2 12 6.8Z", "M12 6.8V20", "M6.5 9c1.4 0 2.6.3 3.5.8 M6.5 12.2c1.4 0 2.6.3 3.5.8 M17.5 9c-1.4 0-2.6.3-3.5.8"), 24f, 2f, 1f)
        DockButton("Tutor", selectedTab == "tutor", onTutor, listOf(
            "M12 5v2.5", "M4.5 7.5h15v12h-15Z", "M2.5 12v3.5 M21.5 12v3.5", "M10.2 16.4c1 .7 2.6.7 3.6 0",
        ), 24f, 2f, 1f)
        Column(Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(20.dp)).clickable(onClick=onProfile).clearAndSetSemantics {
            contentDescription="Perfil";role=Role.Button;selected=selectedTab=="profile";onClick("Perfil") { onProfile();true }
        },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Box(Modifier.size(36.dp).border(3.dp,if(selectedTab=="profile") Color(0xFF2D63D6) else Color(0xFFD5DBE4),CircleShape)
                .padding(3.dp),contentAlignment=Alignment.Center) { AvatarView(look,30.dp) }
            Text("Perfil",color=if(selectedTab=="profile") Color(0xFF2D63D6) else Color(0xFF6B7890),
                fontFamily=Nunito,fontSize=13.sp,fontWeight=if(selectedTab=="profile") FontWeight.Black else FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DockButton(
    label: String, selected: Boolean, onClick: () -> Unit, paths: List<String>, viewBox: Float, strokeWidth: Float, iconFraction: Float,
    extra: Pair<List<String>, Float>? = null,
) {
    val color = if (selected) Color(0xFF2D63D6) else Color(0xFF8A96AB)
    Column(
        Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).clearAndSetSemantics {
            contentDescription = label; role = Role.Button
            this.selected = selected
            onClick(label = label) { onClick(); true }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            DockVisual(label,color)
        }
        Text(label, color = if(selected) Color(0xFF2D63D6) else Color(0xFF6B7890),fontFamily=Nunito,
            fontSize = 13.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.ExtraBold)
    }
}

@Composable
private fun DockVisual(label:String,color:Color) {
    Canvas(Modifier.size(30.dp)) {
        val u=size.width/24f
        fun path(d:String)=PathParser().parsePathString(d).toPath()
        withTransform({ scale(u,u,pivot=androidx.compose.ui.geometry.Offset.Zero) }) {
            val fill=Color(0xFFEEF1F6)
            val line=Stroke(width=2f,cap=StrokeCap.Round,join=StrokeJoin.Round)
            if(label=="Tape") {
                val book=path("M12 6.8C10 5.2 7 4.6 3.5 5v13.2c3.5-.4 6.5.2 8.5 1.8 2-1.6 5-2.2 8.5-1.8V5C17 4.6 14 5.2 12 6.8Z")
                drawPath(book,fill);drawPath(book,color,style=line)
                drawPath(path("M12 6.8V20 M6.5 9c1.4 0 2.6.3 3.5.8 M6.5 12.2c1.4 0 2.6.3 3.5.8 M17.5 9c-1.4 0-2.6.3-3.5.8"),color,style=line)
            } else {
                drawCircle(color,1.4f,androidx.compose.ui.geometry.Offset(12f,3.6f))
                drawPath(path("M12 5v2.5 M2.5 12v3.5 M21.5 12v3.5"),color,style=line)
                drawRoundRect(fill,topLeft=androidx.compose.ui.geometry.Offset(4.5f,7.5f),size=androidx.compose.ui.geometry.Size(15f,12f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(5.5f))
                drawRoundRect(color,topLeft=androidx.compose.ui.geometry.Offset(4.5f,7.5f),size=androidx.compose.ui.geometry.Size(15f,12f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(5.5f),style=line)
                drawCircle(color,1.5f,androidx.compose.ui.geometry.Offset(9.3f,13f))
                drawCircle(color,1.5f,androidx.compose.ui.geometry.Offset(14.7f,13f))
                drawPath(path("M10.2 16.4c1 .7 2.6.7 3.6 0"),color,style=line)
            }
        }
    }
}
