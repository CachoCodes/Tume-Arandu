package com.guarani.mathdemo.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guarani.mathdemo.course.ConceptBlock
import com.guarani.mathdemo.course.Lesson
import kotlinx.coroutines.delay

// @spec spec://modules/learning/FEAT-010-learning-demo#lesson-content
@Composable
internal fun ConceptScreen(
    lesson: Lesson,
    spanishLesson: Lesson?,
    initialReveal: Int,
    completed: Boolean,
    onBack: () -> Unit,
    onReveal: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    var spanish by rememberSaveable(lesson.id) { mutableStateOf(false) }
    val copy = if (spanish) spanishLesson ?: lesson else lesson
    val blocks = copy.concept
    var revealed by rememberSaveable(lesson.id) { mutableIntStateOf(initialReveal.coerceIn(0, blocks.lastIndex)) }
    var picked by rememberSaveable(lesson.id, revealed) { mutableStateOf<String?>(null) }
    var result by rememberSaveable(lesson.id, revealed) { mutableStateOf<Boolean?>(null) }
    var showSolution by rememberSaveable(lesson.id, revealed) { mutableStateOf(false) }
    val scroll = rememberScrollState()
    LaunchedEffect(lesson.id, revealed) { onReveal(revealed); delay(100); scroll.animateScrollTo(scroll.maxValue) }
    LaunchedEffect(result) { if (result != null) { delay(100); scroll.animateScrollTo(scroll.maxValue) } }
    val active = blocks[revealed]
    val check = active.check
    fun advance() { if (revealed == blocks.lastIndex) onFinish() else revealed++ }

    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding()) {
        ScreenHeader(copy.title, spanish, { spanish = !spanish }, onBack)
        LinearProgressIndicator(progress = { (revealed + 1f) / blocks.size }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Accent)
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 24.dp)) {
            if (completed) TextButton(onClick = onFinish, modifier = Modifier.align(Alignment.End)) {
                Text(if (spanish) "Ir a los ejercicios" else "Tembiaporãme")
            }
            blocks.take(revealed + 1).forEachIndexed { index, block ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 20.dp), color = Color(0xFFDCE5ED))
                Text(block.title, Modifier.padding(top = if (index == 0) 22.dp else 0.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                Text(block.text, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge, color = Ink)
                if (block.visual == "compare_figures" && index == revealed && block.check?.figure == true) {
                    FigureChoices(block, picked, { picked = it }, spanish)
                } else ConceptArt(block.visual, spanish)
                if (index < revealed && block.check != null) Text("✓", color = Color(0xFF237A4B), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.fillMaxWidth().background(if (result == true) Color(0xFFE9F7EE) else if (result == false) Color(0xFFFFEDE8) else Color.White).navigationBarsPadding().padding(16.dp)) {
            if (result != null) {
                Text((if (result == true) "✓  " else "↻  ") + if (showSolution) check!!.good else if (result == true) check!!.good else check!!.bad,
                    color = Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showSolution = true }) { Text(if (spanish) "Mostrar solución" else "Ehecha mbohovái") }
            }
            if (check != null && !check.figure && result == null) {
                check.options.forEach { option ->
                    val chosen = picked == option.id
                    Box(Modifier.fillMaxWidth().padding(bottom = 8.dp).border(2.dp, if (chosen) Accent else Color(0xFFB8C9DA), RoundedCornerShape(14.dp))
                        .clickable { picked = option.id }.semantics { role = Role.RadioButton; selected = chosen }.padding(14.dp)) {
                        Text(option.text, color = Ink)
                    }
                }
            }
            val label = when {
                result == false -> if (spanish) "Corregir respuesta" else "Emyatyrõ mbohovái"
                result == true || check == null -> if (revealed == blocks.lastIndex) { if (spanish) "Ir a los ejercicios" else "Tembiaporãme" } else { if (spanish) "Continuar" else "Eho tenonde" }
                else -> if (spanish) "Comprobar respuesta" else "Ehecha mbohovái"
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.size(52.dp)) // Reserved for the tutor robot.
                Box(Modifier.weight(1f)) {
                    PrimaryAction(label, onClick = {
                        when {
                            result == false -> { result = null; picked = null; showSolution = false }
                            result == true || check == null -> advance()
                            else -> result = picked == check.correctOptionId
                        }
                    }, enabled = check == null || result != null || picked != null)
                }
            }
        }
    }
}

@Composable
private fun FigureChoices(block: ConceptBlock, picked: String?, choose: (String) -> Unit, spanish: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        block.check!!.options.forEach { option ->
            val chosen = picked == option.id
            Column(Modifier.weight(1f).border(2.dp, if (chosen) Accent else Color(0xFFCDD8E6), RoundedCornerShape(14.dp))
                .clickable { choose(option.id) }.semantics { role = Role.RadioButton; selected = chosen; contentDescription = option.text }
                .padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(option.text, fontWeight = FontWeight.Bold, color = Ink)
                ConceptArt(if (option.id == "a") "right_angle" else "figure_b", spanish, compact = true)
            }
        }
    }
}

@Composable
private fun ConceptArt(visual: String, spanish: Boolean, compact: Boolean = false) {
    if (visual == "rotated_180" && !compact) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (spanish) "Antes" else "Mboyve", color = Ink)
                ConceptArt("right_angle", spanish, compact = true)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (spanish) "Después de 180°" else "180° rire", color = Ink)
                ConceptArt("rotated_180_single", spanish, compact = true)
            }
        }
        return
    }
    val description = when (visual) {
        "compare_figures" -> if (spanish) "Dos triángulos, el primero con ángulo recto" else "Mokõi triángulo, peteĩha oguereko ángulo recto"
        "ramp", "ramp_right", "ramp_hypotenuse" -> if (spanish) "Esquema de una rampa y un triángulo rectángulo" else "Rampa ha triángulo rectángulo ra'anga"
        "similar_triangles" -> if (spanish) "Dos triángulos semejantes de lados 3, 4, 5 y 6, 8, 10" else "Mokõi triángulo ojoguáva, lado 3, 4, 5 ha 6, 8, 10"
        "tree_shadow" -> if (spanish) "Árbol vertical, sombra horizontal y línea de luz inclinada" else "Yvyra oñembo'y, kuarahy'ã yvýre ha tesape línea"
        else -> if (spanish) "Esquema de un triángulo rectángulo" else "Triángulo rectángulo ra'anga"
    }
    Canvas(Modifier.fillMaxWidth().height(if (compact) 105.dp else 205.dp).padding(vertical = 12.dp).semantics { contentDescription = description }) {
        val sx = size.width / 320f; val sy = size.height / 205f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        val width = 4.dp.toPx()
        if (visual == "similar_triangles") {
            for ((x, scale, labels) in listOf(Triple(30f, .65f, listOf("4", "3", "5")), Triple(170f, 1.3f, listOf("8", "6", "10")))) {
                val left = p(x, 168f); val right = p(x + 104f * scale, 168f); val top = p(x + 104f * scale, 168f - 78f * scale)
                drawLine(Accent, left, right, width); drawLine(Accent, right, top, width); drawLine(Color(0xFFEEA44B), left, top, width)
                drawIntoCanvas { canvas ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(30,43,67); textSize = 15.dp.toPx(); isFakeBoldText = true }
                    canvas.nativeCanvas.drawText(labels[0], (x + 45f * scale) * sx, 192f * sy, paint)
                    canvas.nativeCanvas.drawText(labels[1], (x + 109f * scale) * sx, (168f - 37f * scale) * sy, paint)
                    canvas.nativeCanvas.drawText(labels[2], (x + 40f * scale) * sx, (168f - 53f * scale) * sy, paint)
                }
            }
            return@Canvas
        }
        if (visual == "tree_shadow") {
            drawLine(Color(0xFF7F9C75), p(24f, 164f), p(290f, 164f), 8.dp.toPx())
            drawLine(Accent, p(244f, 164f), p(244f, 28f), 12.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(Color(0xFF7CC957), 27.dp.toPx(), p(244f, 29f))
            drawLine(Color(0xFFEEA44B), p(55f, 164f), p(244f, 28f), width)
            drawLine(Color(0xFFEEA44B), p(55f, 164f), p(244f, 164f), width)
            drawIntoCanvas { canvas ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(30,43,67); textSize = 17.dp.toPx(); isFakeBoldText = true }
                canvas.nativeCanvas.drawText(if (spanish) "sombra" else "kuarahy'ã", 95f * sx, 190f * sy, paint)
                canvas.nativeCanvas.drawText(if (spanish) "altura" else "yvate", 252f * sx, 105f * sy, paint)
            }
            return@Canvas
        }
        val rotated = visual.startsWith("rotated_180")
        val b = if (rotated) p(42f, 33f) else p(270f, 163f)
        val a = if (rotated) p(284f, 33f) else p(28f, 163f)
        val c = if (rotated) p(42f, 178f) else if (visual == "figure_b") p(160f, 18f) else p(270f, 18f)
        val baseColor = if (visual in setOf("hypotenuse", "sine_ratio")) Color(0xFFBFD5E7) else if (visual in setOf("cosine_ratio", "tangent_ratio")) Color(0xFFEEA44B) else Accent
        val oppositeColor = if (visual in setOf("sine_ratio", "tangent_ratio")) Color(0xFFEEA44B) else Accent
        val hypColor = if (visual in setOf("hypotenuse", "ramp_hypotenuse", "sine_ratio", "cosine_ratio")) Color(0xFFEEA44B) else Accent
        if (visual.startsWith("ramp")) {
            drawLine(Color(0xFFC5D7E7), p(20f, 164f), p(286f, 164f), 12.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color(0xFFEEA44B), p(30f, 164f), p(270f, 22f), 9.dp.toPx(), cap = StrokeCap.Round)
        }
        drawLine(baseColor, a, b, width, cap = StrokeCap.Round)
        drawLine(oppositeColor, b, c, width, cap = StrokeCap.Round)
        drawLine(hypColor, a, c, width, cap = StrokeCap.Round)
        if (visual != "triangle_vertices" && visual != "figure_b") {
            val m = 22f
            if (rotated) {
                drawLine(Color(0xFFEEA44B), b + p(0f, m), b + p(m, m), 3.dp.toPx())
                drawLine(Color(0xFFEEA44B), b + p(m, m), b + p(m, 0f), 3.dp.toPx())
            } else {
                drawLine(Color(0xFFEEA44B), b - p(m, 0f), b - p(m, m), 3.dp.toPx())
                drawLine(Color(0xFFEEA44B), b - p(m, m), b - p(0f, m), 3.dp.toPx())
            }
        }
        if (visual == "triangle_vertices") listOf(a,b,c).forEach { drawCircle(Color(0xFFEEA44B), 5.dp.toPx(), it) }
        val labels = when (visual) {
            "known_sides", "unknown_side" -> listOf(Triple("4",154f,191f),Triple("?",291f,99f),Triple("5",140f,80f))
            "other_numbers" -> listOf(Triple("6",154f,191f),Triple("?",291f,99f),Triple("10",140f,80f))
            "labeled_sides" -> listOf(Triple("A",154f,191f),Triple("B",291f,99f),Triple("C",140f,80f))
            "angle_arc" -> listOf(Triple("α",52f,155f),Triple("90°",266f,184f))
            "sine_ratio", "cosine_ratio", "tangent_ratio", "ratio_choice" -> listOf(Triple("4",154f,191f),Triple("3",291f,99f),Triple("5",140f,80f),Triple("α",48f,153f))
            "ramp", "ramp_right", "ramp_hypotenuse" -> listOf(Triple(if (spanish) "base" else "base", 100f, 191f), Triple(if (spanish) "altura" else "altura", 275f, 100f))
            else -> emptyList()
        }
        if (labels.isNotEmpty()) drawIntoCanvas { canvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(30,43,67); textSize = 20.dp.toPx(); isFakeBoldText = true }
            labels.forEach { (s,x,y) -> canvas.nativeCanvas.drawText(s, x*sx, y*sy, paint) }
        }
    }
}
