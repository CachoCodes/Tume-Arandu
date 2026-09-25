package com.guarani.mathdemo.ui

import android.graphics.Paint as NativePaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guarani.mathdemo.R
import com.guarani.mathdemo.course.ChoiceExercise
import com.guarani.mathdemo.course.Course
import com.guarani.mathdemo.course.CourseLoader
import com.guarani.mathdemo.course.Exercise
import com.guarani.mathdemo.course.InputExercise
import com.guarani.mathdemo.course.Lesson
import com.guarani.mathdemo.course.MatchingExercise
import com.guarani.mathdemo.course.StepExercise
import com.guarani.mathdemo.exercise.validateAnswer
import com.guarani.mathdemo.exercise.validateMatching
import com.guarani.mathdemo.exercise.validateStep
import com.guarani.mathdemo.progress.ExerciseResult
import com.guarani.mathdemo.progress.ProgressRepository
import com.guarani.mathdemo.progress.ProgressState
import com.guarani.mathdemo.progress.ProgressSnapshot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import java.math.BigDecimal

// @spec spec://modules/learning/FEAT-010-learning-demo#flow
@Composable
fun CourseApp(repository: ProgressRepository) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val course = remember { runCatching { CourseLoader.load(context) }.getOrNull() }
    val spanishLessons = remember {
        runCatching { CourseLoader.load(context, "es").lessons.associateBy(Lesson::id) }.getOrDefault(emptyMap())
    }
    val progressState by repository.progress.collectAsState(initial = ProgressState.Loading)
    val progress = (progressState as? ProgressState.Ready)?.snapshot
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    fun selectTab(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (course == null || progress == null) {
        RecoveryScreen(loading = course != null && progressState == ProgressState.Loading)
        return
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        NavHost(navController = nav, startDestination = "map") {
            composable("map") {
                CourseMapScreen(
                    course = course,
                    progress = progress,
                    selectedTab = "map",
                    onMap = { scope.launch { nav.popBackStack("map", inclusive = false) } },
                    onTutor = { selectTab("tutor") },
                    onProfile = { selectTab("profile") },
                    onOpen = { lesson ->
                        val firstExercise = progress.currentExerciseId
                            ?.takeIf { progress.currentLessonId == lesson.id && lesson.exercises.any { it.id == progress.currentExerciseId } }
                            ?: lesson.exercises.firstOrNull { progress.results[it.id]?.isTerminal != true }?.id
                            ?: lesson.exercises.first().id
                        scope.launch { repository.saveCursor(lesson.id, firstExercise) }
                        nav.navigate("lesson/${lesson.id}")
                    },
                )
            }
            composable("tutor") {
                TutorScreen(
                    courseTitle = course.title,
                    selectedTab = "tutor",
                    onMap = { selectTab("map") },
                    onTutor = { selectTab("tutor") },
                    onProfile = { selectTab("profile") },
                )
            }
            composable("profile") {
                val exercises = course.lessons.flatMap { it.exercises }
                val answered = exercises.count { progress.results[it.id]?.isTerminal == true }
                val correct = exercises.count { progress.results[it.id]?.correct == true }
                val mastery = if (answered == 0) 0 else correct * 100 / answered
                ProfileScreen(
                    course = course,
                    progress = progress,
                    mastery = mastery,
                    selectedTab = "profile",
                    onMap = { selectTab("map") },
                    onTutor = { selectTab("tutor") },
                    onProfile = { selectTab("profile") },
                )
            }
            composable(
                route = "lesson/{lessonId}",
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
            ) { entry ->
                val lesson = course.lessons.firstOrNull { it.id == entry.arguments?.getString("lessonId") }
                    ?: return@composable
                LessonScreen(
                    lesson = lesson,
                    spanishLesson = spanishLessons[lesson.id],
                    progress = progress,
                    repository = repository,
                    onBack = { nav.popBackStack() },
                    onFinish = {
                        val award = if (lesson.id in progress.completedLessonIds) 0 else lesson.xpReward
                        scope.launch {
                            repository.completeLesson(lesson)
                            nav.navigate("complete/${lesson.id}/$award") {
                                popUpTo("map") { inclusive = false }
                            }
                        }
                    },
                )
            }
            composable(
                route = "complete/{lessonId}/{award}",
                arguments = listOf(
                    navArgument("lessonId") { type = NavType.StringType },
                    navArgument("award") { type = NavType.IntType },
                ),
            ) { entry ->
                val lesson = course.lessons.first { it.id == entry.arguments?.getString("lessonId") }
                CompletionScreen(lesson, progress, entry.arguments?.getInt("award") ?: 0) {
                    nav.popBackStack("map", inclusive = false)
                }
            }
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#course-map
@Composable
private fun CourseMapScreen(
    course: Course,
    progress: ProgressSnapshot,
    selectedTab: String,
    onMap: () -> Unit,
    onTutor: () -> Unit,
    onProfile: () -> Unit,
    onOpen: (Lesson) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MapBackground).statusBarsPadding().navigationBarsPadding()) {
        CourseTopPlaque(course, progress)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            CoursePath(
                course = course,
                progress = progress,
                onOpen = onOpen,
            )
        }
        CourseBottomBar(
            selectedTab = selectedTab,
            onMap = onMap,
            onTutor = onTutor,
            onProfile = onProfile,
        )
    }
}

@Composable
private fun CourseTopPlaque(course: Course, progress: ProgressSnapshot) {
    val completion = progress.completedLessonIds.size.toFloat() / course.lessons.size.coerceAtLeast(1)
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCE9FC)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 14.dp, top = 9.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2B77D5), Color(0xFF57BEEB)))),
                contentAlignment = Alignment.Center,
            ) {
                Text("∠", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text(course.title, color = Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text("Tape ñemoarandu", color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${progress.completedLessonIds.size}/${course.lessons.size}",
                    color = Accent,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text("MBO'EPY", color = Muted, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
            }
        }
        LinearProgressIndicator(
            progress = { completion.coerceIn(0f, 1f) },
            Modifier.fillMaxWidth().height(3.dp),
            color = Color(0xFF43B9E8),
            trackColor = Color(0xFFEAF2FC),
        )
    }
}

@Composable
private fun CourseBottomBar(
    selectedTab: String,
    onMap: () -> Unit,
    onTutor: () -> Unit,
    onProfile: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCE9FC)),
    ) {
        Row(
            Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 9.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockAction("Tape", selected = selectedTab == "map", icon = "⌁", onClick = onMap, modifier = Modifier.weight(1f))
            TutorDockAction(selected = selectedTab == "tutor", onClick = onTutor, modifier = Modifier.weight(1f))
            DockAction("Perfil", selected = selectedTab == "profile", icon = "M", onClick = onProfile, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DockAction(label: String, selected: Boolean, icon: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.clickable(onClick = onClick).clearAndSetSemantics {
            contentDescription = label
            role = Role.Button
            onClick(label = label) { onClick(); true }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon == "M") {
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFEAF3FF)), contentAlignment = Alignment.Center) {
                Text(icon, color = Accent, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
            }
        } else {
            Text(icon, color = if (selected) Accent else Muted, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(label, Modifier.padding(top = 1.dp), color = if (selected) Accent else Muted, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun TutorDockAction(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.clickable(onClick = onClick).clearAndSetSemantics {
            contentDescription = "Tutor"
            role = Role.Button
            onClick(label = "Tutor") { onClick(); true }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            Modifier.size(45.dp), shape = CircleShape,
            color = if (selected) Accent else Color(0xFFEAF3FF),
            shadowElevation = if (selected) 4.dp else 0.dp,
        ) {
            Canvas(Modifier.padding(11.dp)) {
                val stroke = 2.dp.toPx()
                val iconColor = if (selected) Color.White else Accent
                drawRoundRect(
                    iconColor,
                    topLeft = Offset(size.width * .13f, size.height * .24f),
                    size = androidx.compose.ui.geometry.Size(size.width * .74f, size.height * .58f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
                    style = Stroke(stroke),
                )
                drawLine(iconColor, Offset(size.width * .5f, size.height * .24f), Offset(size.width * .5f, size.height * .08f), stroke, cap = StrokeCap.Round)
                drawCircle(iconColor, radius = stroke * .9f, center = Offset(size.width * .5f, size.height * .05f))
                drawCircle(iconColor, radius = stroke * .75f, center = Offset(size.width * .37f, size.height * .49f))
                drawCircle(iconColor, radius = stroke * .75f, center = Offset(size.width * .63f, size.height * .49f))
                drawLine(iconColor, Offset(size.width * .38f, size.height * .68f), Offset(size.width * .62f, size.height * .68f), stroke, cap = StrokeCap.Round)
            }
        }
        Text("Tutor", Modifier.padding(top = 1.dp), color = if (selected) Accent else Muted, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium)
    }
}

// @spec spec://modules/android/PROP-010-android-demo-architecture#navigation
@Composable
private fun ProfileScreen(
    course: Course,
    progress: ProgressSnapshot,
    mastery: Int,
    selectedTab: String,
    onMap: () -> Unit,
    onTutor: () -> Unit,
    onProfile: () -> Unit,
) {
    val completion = progress.completedLessonIds.size.toFloat() / course.lessons.size.coerceAtLeast(1)
    Column(Modifier.fillMaxSize().background(MapBackground).statusBarsPadding().navigationBarsPadding()) {
        StudyPageHeader("Perfil", "Ñemoarandu oñeñongatu ko dispositivo-pe", "M")
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp),
                color = Color(0xFF173F68), shadowElevation = 5.dp,
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(62.dp).clip(CircleShape).background(Color(0xFF3C83B5)), contentAlignment = Alignment.Center) {
                        Text("M", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    }
                    Column(Modifier.padding(start = 15.dp)) {
                        Text("Mba'ekuaa rape", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(course.title, Modifier.padding(top = 3.dp), color = Color(0xFFBBD7EC), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Surface(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD7E6F1)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatBlock("${progress.xp}", "XP", "Oñegana")
                    StatBlock("${progress.completedLessonIds.size}/${course.lessons.size}", "Mbo'epy", "Oñemohu'ã")
                    StatBlock("$mastery%", "Mba'ekuaa", "Jehechajey")
                }
            }
            Surface(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD7E6F1)),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${course.title}", Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${progress.completedLessonIds.size}/${course.lessons.size}", color = Accent, fontWeight = FontWeight.ExtraBold)
                    }
                    LinearProgressIndicator(
                        progress = { completion.coerceIn(0f, 1f) },
                        Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                        color = Color(0xFF48B9DF), trackColor = Color(0xFFE4EEF5),
                    )
                    Text("${mastery}% · Mba'ekuaa", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Mbo'epykuéra", color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            course.lessons.forEachIndexed { index, lesson ->
                val completed = lesson.id in progress.completedLessonIds
                val available = index == 0 || course.lessons.getOrNull(index - 1)?.id in progress.completedLessonIds
                val current = progress.currentLessonId == lesson.id
                val itemColor = when {
                    completed -> Color(0xFF228477)
                    current -> Color(0xFF2D89C2)
                    available -> Accent
                    else -> Color(0xFF9AABBA)
                }
                val itemStatus = when {
                    completed -> "Oñemohu'ãma"
                    current -> "Ejesegi ko'ápe"
                    available -> "Eñepyrũ"
                    else -> "Oñemboty"
                }
                Surface(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCE8F0)),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(itemColor.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                            Text(if (completed) "✓" else "${index + 1}", color = itemColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                        }
                        Column(Modifier.weight(1f).padding(start = 11.dp)) {
                            Text(lesson.title, color = Ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(itemStatus, color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                        Text("›", color = itemColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        CourseBottomBar(selectedTab, onMap, onTutor, onProfile)
    }
}

// @spec spec://modules/android/PROP-010-android-demo-architecture#navigation
@Composable
private fun TutorScreen(
    courseTitle: String,
    selectedTab: String,
    onMap: () -> Unit,
    onTutor: () -> Unit,
    onProfile: () -> Unit,
) {
    val messages = remember {
        mutableStateListOf(
            TutorMessage("Maitei. Mba'épepa ikatu roipytyvõ ko mbo'epy rehe?", fromTutor = true),
            TutorMessage("Ikatu ñañe'ẽ seno, coseno ha ángulo recto rehe.", fromTutor = true),
        )
    }
    val chatScroll = rememberScrollState()
    var draft by rememberSaveable { mutableStateOf("") }
    fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty()) return
        messages += TutorMessage(message, fromTutor = false)
        messages += TutorMessage(demoTutorReply(message), fromTutor = true)
        draft = ""
    }
    LaunchedEffect(messages.size) { chatScroll.animateScrollTo(chatScroll.maxValue) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF3F8FC)).statusBarsPadding().navigationBarsPadding().imePadding()) {
        StudyPageHeader("Tutor", courseTitle, "T", status = "DEMO · LOCAL")
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 13.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(23.dp)).background(Color(0xFFEAF2F8))
                .verticalScroll(chatScroll).padding(horizontal = 13.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            messages.forEach { message -> TutorBubble(message) }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Seno 30°", "Coseno 30°", "Ángulo recto").forEach { topic ->
                Surface(
                    Modifier.clickable { send(topic) }, shape = RoundedCornerShape(18.dp), color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4E3EF)),
                ) {
                    Text(topic, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = Ink, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ehai ne porandu...") },
                shape = RoundedCornerShape(23.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(draft) }),
            )
            Button(
                onClick = { send(draft) }, enabled = draft.isNotBlank(),
                modifier = Modifier.padding(start = 8.dp).size(52.dp),
                shape = CircleShape, contentPadding = PaddingValues(0.dp),
            ) { Text("↑", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        }
        CourseBottomBar(selectedTab, onMap, onTutor, onProfile)
    }
}

private data class TutorMessage(val text: String, val fromTutor: Boolean)

@Composable
private fun TutorBubble(message: TutorMessage) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromTutor) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (message.fromTutor) {
            Surface(Modifier.size(27.dp), shape = CircleShape, color = Color(0xFF2B77A8)) {
                TutorMark(Modifier.padding(7.dp))
            }
        }
        Surface(
            Modifier.padding(horizontal = 7.dp).widthIn(max = 292.dp),
            color = if (message.fromTutor) Color.White else Color(0xFF1B5E8A),
            shape = RoundedCornerShape(
                topStart = if (message.fromTutor) 6.dp else 19.dp,
                topEnd = if (message.fromTutor) 19.dp else 6.dp,
                bottomStart = 19.dp,
                bottomEnd = 19.dp,
            ),
            shadowElevation = if (message.fromTutor) 1.dp else 0.dp,
        ) {
            Text(
                message.text, Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                color = if (message.fromTutor) Ink else Color.White, style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun demoTutorReply(question: String): String {
    val query = question.lowercase()
    return when {
        "cos" in query || "√3" in query || "adyacente" in query ->
            "Coseno ha'e lado adyacente ÷ hipotenusa. 30°-pe: √3 ÷ 2 = √3/2."
        "seno" in query || "sen" in query || "sin" in query || "opuesto" in query ->
            "Seno ha'e lado opuesto ÷ hipotenusa. 30°-pe: 1 ÷ 2 = 1/2."
        "90" in query || "recto" in query ->
            "Ángulo recto oguereko 90°. Pe cuadrado ohechauka oñojuha mokõi lado."
        else ->
            "Ko demo ikatu omyesakã seno, coseno ha ángulo recto. Eporandu peteĩva ko'ã tema rehe."
    }
}

@Composable
private fun StudyPageHeader(title: String, subtitle: String, mark: String, status: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFF173F68)),
            contentAlignment = Alignment.Center,
        ) {
            if (mark == "T") TutorMark(Modifier.padding(11.dp))
            else Text(mark, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        if (status != null) {
            Surface(color = Color(0xFFE5F2E9), shape = RoundedCornerShape(11.dp)) {
                Text(status, Modifier.padding(horizontal = 8.dp, vertical = 6.dp), color = Color(0xFF477354), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TutorMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 2.dp.toPx()
        drawRoundRect(
            Color.White,
            topLeft = Offset(size.width * .12f, size.height * .25f),
            size = androidx.compose.ui.geometry.Size(size.width * .76f, size.height * .58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
            style = Stroke(stroke),
        )
        drawCircle(Color.White, radius = stroke, center = Offset(size.width * .38f, size.height * .5f))
        drawCircle(Color.White, radius = stroke, center = Offset(size.width * .62f, size.height * .5f))
    }
}

@Composable
private fun StatBlock(kicker: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(kicker, color = Accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(value, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#course-map
private data class CourseMapNode(
    val lesson: Lesson,
    val index: Int,
    val completed: Boolean,
    val unlocked: Boolean,
    val current: Boolean,
    val status: String,
)

private val courseMapColumns = listOf(.33f, .50f, .67f, .67f, .50f, .33f, .33f, .50f, .67f, .67f)

private fun courseMapNodeCenters(count: Int, width: Float, height: Float, density: Float): List<Offset> {
    if (count == 0) return emptyList()
    val inset = min(44f * density, height * .10f)
    val top = inset
    val bottom = height - inset
    val denominator = (count - 1).coerceAtLeast(1)
    return List(count.coerceAtMost(10)) { index ->
        val progress = if (count == 1) .5f else index / denominator.toFloat()
        Offset(
            x = width * courseMapColumns[index % courseMapColumns.size],
            y = bottom - (bottom - top) * progress,
        )
    }
}

private fun courseMapTileWidth(count: Int, width: Float, height: Float, density: Float): Float {
    val inset = min(44f * density, height * .10f)
    val spacing = (height - inset * 2f) / (count - 1).coerceAtLeast(1)
    return minOf(width * .20f, 74f * density, spacing * .90f)
}

@Composable
private fun CoursePath(
    course: Course,
    progress: ProgressSnapshot,
    onOpen: (Lesson) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val lessons = course.lessons.take(10)
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val centers = courseMapNodeCenters(lessons.size, widthPx, heightPx, density.density)
        val nodes = lessons.mapIndexed { index, lesson ->
            val completed = lesson.id in progress.completedLessonIds
            val unlocked = index == 0 || course.lessons.getOrNull(index - 1)?.id in progress.completedLessonIds
            val current = progress.currentLessonId == lesson.id && !completed
            val status = when {
                completed -> "Oñemohu'ãma"
                !unlocked -> "Oñemboty"
                current -> "Ejesegi ko'ápe"
                else -> "Eñepyrũ"
            }
            CourseMapNode(lesson, index, completed, unlocked, current, status)
        }

        Canvas(Modifier.fillMaxSize()) {
            drawCourseMapBackground()
            val canvasCenters = courseMapNodeCenters(nodes.size, size.width, size.height, density.density)
            drawCourseMapRoute(canvasCenters)
            val propWidth = minOf(size.width * .38f, 148.dp.toPx())
            drawTriangleMapProp(
                centerX = max(propWidth / 2f + 8.dp.toPx(), size.width * .16f),
                centerY = size.height * .61f,
                width = propWidth,
                title = "SIN",
                formula = "sin 30° = 1/2",
                highlightVertical = true,
                rotation = -6f,
            )
            drawTriangleMapProp(
                centerX = size.width - max(propWidth / 2f + 8.dp.toPx(), size.width * .16f),
                centerY = size.height * .37f,
                width = propWidth,
                title = "COS",
                formula = "cos 30° = √3/2",
                highlightVertical = false,
                rotation = 6f,
            )
            val tileWidth = courseMapTileWidth(nodes.size, size.width, size.height, density.density)
            nodes.forEachIndexed { index, node ->
                drawCourseMapTile(canvasCenters[index], tileWidth, node)
            }
        }

        val tileWidthPx = courseMapTileWidth(nodes.size, widthPx, heightPx, density.density)
        val touchSize = with(density) { minOf(64.dp, maxOf(48.dp, (tileWidthPx * 1.18f).toDp())) }
        nodes.forEachIndexed { index, node ->
            val center = centers[index]
            val x = with(density) { center.x.toDp() } - touchSize / 2
            val y = with(density) { center.y.toDp() } - touchSize / 2
            Box(
                Modifier.absoluteOffset(x = x, y = y)
                    .size(touchSize)
                    .clickable(enabled = node.unlocked) { onOpen(node.lesson) }
                    .clearAndSetSemantics {
                        contentDescription = "Mbo'epy ${node.index + 1}. ${node.lesson.title}. ${node.status}"
                        role = Role.Button
                        if (!node.unlocked) disabled()
                        onClick(label = "Eike mbo'epy ${node.lesson.title}") {
                            if (node.unlocked) onOpen(node.lesson)
                            node.unlocked
                        }
                    },
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCourseMapBackground() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFEAF4F8), Color(0xFFD4E5ED), Color(0xFFC2D9E5)),
            endY = size.height,
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = .48f), Color.Transparent),
            center = Offset(size.width * .5f, size.height * .4f),
            radius = max(size.width, size.height) * .72f,
        ),
    )

    val cellWidth = 76.dp.toPx()
    val cellHeight = cellWidth * (52f / 74f)
    val run = size.height * cellWidth / cellHeight
    var intercept = -run
    val grid = Color(0x2D436F8C)
    while (intercept <= size.width + run) {
        drawLine(grid, Offset(intercept, 0f), Offset(intercept + run, size.height), 1.dp.toPx())
        drawLine(grid, Offset(intercept, 0f), Offset(intercept - run, size.height), 1.dp.toPx())
        intercept += cellWidth
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCourseMapRoute(points: List<Offset>) {
    points.zipWithNext().forEach { (start, end) ->
        val lowerStart = start + Offset(0f, 6.dp.toPx())
        val lowerEnd = end + Offset(0f, 6.dp.toPx())
        drawLine(Color(0x3842698D), lowerStart, lowerEnd, 12.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color(0xCCF8FDFF), start, end, 8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color(0xFF69BFE2), start, end, 3.dp.toPx(), cap = StrokeCap.Round)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCourseMapTile(
    center: Offset,
    width: Float,
    node: CourseMapNode,
) {
    val topHeight = width * .68f
    val halfWidth = width / 2f
    val halfHeight = topHeight / 2f
    val depth = 7.dp.toPx()
    val extrude = Offset(depth * .48f, depth)
    val top = Offset(center.x, center.y - halfHeight)
    val right = Offset(center.x + halfWidth, center.y)
    val bottom = Offset(center.x, center.y + halfHeight)
    val left = Offset(center.x - halfWidth, center.y)
    val topFace = Path().apply {
        moveTo(top.x, top.y)
        lineTo(right.x, right.y)
        lineTo(bottom.x, bottom.y)
        lineTo(left.x, left.y)
        close()
    }

    val shadowCenter = center + Offset(depth * .4f, depth + 4.dp.toPx())
    drawOval(
        Brush.radialGradient(
            colors = listOf(Color(0x3C315C7D), Color(0x00315C7D)),
            center = shadowCenter,
            radius = width * .66f,
        ),
        topLeft = Offset(shadowCenter.x - width * .58f, shadowCenter.y - topHeight * .23f),
        size = androidx.compose.ui.geometry.Size(width * 1.16f, topHeight * .46f),
    )

    val (leftSide, rightSide, faceStart, faceEnd) = when {
        node.completed -> listOf(Color(0xFF478D4A), Color(0xFF66AA53), Color(0xFFC8EBAA), Color(0xFF8DD271))
        node.current -> listOf(Color(0xFF236EAA), Color(0xFF3F91C8), Color(0xFFF1FBFF), Color(0xFFBDE9F8))
        node.unlocked -> listOf(Color(0xFF236EAA), Color(0xFF3F91C8), Color(0xFFFFFFFF), Color(0xFFD0EAF6))
        else -> listOf(Color(0xFF637D96), Color(0xFF7794A8), Color(0xFFF2F5F6), Color(0xFFD7E2E9))
    }

    drawCourseMapPolygon(listOf(left, bottom, bottom + extrude, left + extrude), leftSide)
    drawCourseMapPolygon(listOf(bottom, right, right + extrude, bottom + extrude), rightSide)
    drawPath(
        topFace,
        Brush.linearGradient(listOf(faceStart, faceEnd), top, bottom),
    )
    drawPath(topFace, Color(0xCCFFFFFF), style = Stroke(width = 1.25.dp.toPx()))
    drawLine(Color(0xFF8AB8D0), left, bottom, 1.1.dp.toPx(), cap = StrokeCap.Round)
    drawLine(Color(0xFF9CC6DB), bottom, right, 1.1.dp.toPx(), cap = StrokeCap.Round)

    drawCourseMapLabel((node.index + 1).toString(), center + Offset(0f, 1.dp.toPx()), 17f, Color(0xFF245271), bold = true)
    val statusPoint = Offset(center.x - width * .23f, center.y - topHeight * .17f)
    when {
        node.completed -> {
            drawCircle(Color(0xFF3D9555), radius = 4.dp.toPx(), center = statusPoint)
            drawLine(Color.White, statusPoint + Offset(-2.dp.toPx(), 0f), statusPoint + Offset(-.5f.dp.toPx(), 1.8f.dp.toPx()), 1.6.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color.White, statusPoint + Offset(-.5f.dp.toPx(), 1.8f.dp.toPx()), statusPoint + Offset(2.3f.dp.toPx(), -1.8f.dp.toPx()), 1.6.dp.toPx(), cap = StrokeCap.Round)
        }
        !node.unlocked -> {
            val lockCenter = Offset(center.x + width * .27f, center.y - topHeight * .18f)
            val lockSize = 5.2f.dp.toPx()
            drawRoundRect(Color(0xFF61798A), Offset(lockCenter.x - lockSize / 2f, lockCenter.y), androidx.compose.ui.geometry.Size(lockSize, lockSize * .72f), androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()))
            drawArc(Color(0xFF61798A), 180f, 180f, false, Offset(lockCenter.x - lockSize * .28f, lockCenter.y - lockSize * .45f), androidx.compose.ui.geometry.Size(lockSize * .56f, lockSize * .65f), style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
        }
        node.current -> drawCircle(Color(0xFFFFAE48), radius = 3.dp.toPx(), center = statusPoint)
        else -> drawCircle(Color(0xFF48A9D5), radius = 2.4.dp.toPx(), center = statusPoint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTriangleMapProp(
    centerX: Float,
    centerY: Float,
    width: Float,
    title: String,
    formula: String,
    highlightVertical: Boolean,
    rotation: Float,
) {
    val baseLength = width * .76f
    val triangleHeight = baseLength / sqrt(3f)
    val left = Offset(centerX - baseLength / 2f, centerY + triangleHeight * .31f)
    val right = Offset(centerX + baseLength / 2f, left.y)
    val apex = Offset(right.x, left.y - triangleHeight)
    val pivot = Offset((left.x + right.x * 2f) / 3f, (left.y * 2f + apex.y) / 3f)
    val depth = Offset(8f.dp.toPx(), 7f.dp.toPx())

    drawOval(
        Brush.radialGradient(
            colors = listOf(Color(0x423A5672), Color(0x003A5672)),
            center = Offset(centerX + 5.dp.toPx(), left.y + 8.dp.toPx()),
            radius = baseLength * .68f,
        ),
        topLeft = Offset(centerX - baseLength * .58f, left.y + 2.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(baseLength * 1.2f, 14.dp.toPx()),
    )

    rotate(rotation, pivot) {
        drawCourseMapPolygon(listOf(left + depth, right + depth, apex + depth), Color(0xFF1D557D))
        drawCourseMapPolygon(listOf(left, right, right + depth, left + depth), Color(0xFF317FAF))
        drawCourseMapPolygon(listOf(right, apex, apex + depth, right + depth), Color(0xFF246A98))

        val face = Path().apply {
            moveTo(left.x, left.y)
            lineTo(right.x, right.y)
            lineTo(apex.x, apex.y)
            close()
        }
        drawPath(face, Brush.linearGradient(listOf(Color(0xFFF8FDFF), Color(0xFFC6E5F3)), apex, left))
        drawPath(face, Color(0xFFEAF8FE), style = Stroke(width = 1.5.dp.toPx()))
        drawLine(Color(0xFF6FA5C4), left, right, 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color(0xFF6FA5C4), right, apex, 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color(0xFF6FA5C4), apex, left, 2.dp.toPx(), cap = StrokeCap.Round)

        val highlightedStart = if (highlightVertical) right else left
        val highlightedEnd = if (highlightVertical) apex else right
        drawLine(Color(0xFFFFA94F), highlightedStart, highlightedEnd, 3.2.dp.toPx(), cap = StrokeCap.Round)

        val angleRadius = 12.dp.toPx()
        drawArc(
            Color(0xFFFFA94F), 330f, 30f, false,
            Offset(left.x, left.y - angleRadius),
            androidx.compose.ui.geometry.Size(angleRadius * 2f, angleRadius * 2f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCourseMapLabel("30°", Offset(left.x + 14.dp.toPx(), left.y - 14.dp.toPx()), 9.5f, Color(0xFF1D4661), bold = true)
        drawCourseMapLabel("90°", Offset(right.x - 16.dp.toPx(), right.y - 15.dp.toPx()), 9.5f, Color(0xFF1D4661), bold = true)
        drawCourseMapLabel("√3", Offset((left.x + right.x) / 2f, left.y - 7.dp.toPx()), 10f, Color(0xFF244B68), bold = true)
        drawCourseMapLabel("1", Offset(right.x - 9.dp.toPx(), (right.y + apex.y) / 2f), 10f, Color(0xFF244B68), bold = true)
        drawCourseMapLabel(
            "2",
            Offset(left.x + baseLength * .54f + 5.dp.toPx(), left.y - triangleHeight * .47f),
            10.5f,
            Color(0xFF244B68),
            bold = true,
            rotation = -30f,
        )
    }

    drawCourseMapLabel(title, Offset(centerX, apex.y - 13.dp.toPx()), 10f, Color(0xFF2773A5), bold = true)
    drawCourseMapLabel(formula, Offset(centerX, left.y + 17.dp.toPx()), 9.5f, Color(0xFF244B68), bold = true)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCourseMapPolygon(points: List<Offset>, color: Color) {
    val polygon = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(polygon, color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCourseMapLabel(
    text: String,
    center: Offset,
    textSizeSp: Float,
    color: Color,
    bold: Boolean,
    rotation: Float = 0f,
) {
    val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        textSize = with(drawContext.density) { textSizeSp.sp.toPx() }
        textAlign = NativePaint.Align.CENTER
        typeface = Typeface.create("sans-serif-rounded", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    val nativeCanvas = drawContext.canvas.nativeCanvas
    val save = nativeCanvas.save()
    nativeCanvas.rotate(rotation, center.x, center.y)
    val metrics = paint.fontMetrics
    val baseline = center.y - (metrics.ascent + metrics.descent) / 2f
    nativeCanvas.drawText(text, center.x, baseline, paint)
    nativeCanvas.restoreToCount(save)
}

// @spec spec://modules/learning/FEAT-010-learning-demo#lesson-content
@Composable
private fun LessonScreen(
    lesson: Lesson,
    spanishLesson: Lesson?,
    progress: ProgressSnapshot,
    repository: ProgressRepository,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    var spanish by rememberSaveable(lesson.id) { mutableStateOf(false) }
    val displayedLesson = if (spanish) spanishLesson ?: lesson else lesson
    val initialExercise = progress.currentExerciseId
        ?.takeIf { progress.currentLessonId == lesson.id && lesson.exercises.any { exercise -> exercise.id == progress.currentExerciseId } }
        ?: lesson.exercises.firstOrNull { progress.results[it.id]?.isTerminal != true }?.id
        ?: lesson.exercises.last().id
    var currentExerciseId by rememberSaveable(lesson.id, progress.currentLessonId, progress.currentExerciseId) {
        mutableStateOf(initialExercise)
    }
    val exercise = displayedLesson.exercises.firstOrNull { it.id == currentExerciseId } ?: displayedLesson.exercises.first()
    val exerciseIndex = displayedLesson.exercises.indexOf(exercise)
    var finishing by rememberSaveable(lesson.id) { mutableStateOf(false) }

    LaunchedEffect(lesson.id, exercise.id) { repository.saveCursor(lesson.id, exercise.id) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(
            title = displayedLesson.title,
            spanish = spanish,
            onToggleLanguage = { spanish = !spanish },
            onBack = onBack,
        )
        ExercisePanel(
            lesson = displayedLesson,
            exercise = exercise,
            spanish = spanish,
            index = exerciseIndex,
            total = displayedLesson.exercises.size,
            progress = progress.results[exercise.id],
            repository = repository,
            onNext = {
                val next = displayedLesson.exercises.drop(exerciseIndex + 1)
                    .firstOrNull { progress.results[it.id]?.isTerminal != true }
                    ?: displayedLesson.exercises.firstOrNull { progress.results[it.id]?.isTerminal != true }
                if (next == null) onFinish() else currentExerciseId = next.id
            },
            onFinish = {
                if (!finishing) {
                    finishing = true
                    onFinish()
                }
            },
        )
    }
}

@Composable
private fun AngleVisual() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        AngleDiagramTile("90°", "Ángulo recto", straight = false, Modifier.weight(1f))
        AngleDiagramTile("180°", "Ángulo llano", straight = true, Modifier.weight(1f))
    }
}

@Composable
private fun AngleDiagramTile(value: String, label: String, straight: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(17.dp)).background(Color(0xFFF2F7FD))
            .border(1.dp, Color(0xFFE1EBF8), RoundedCornerShape(17.dp)).padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.fillMaxWidth().height(70.dp)) {
            val line = Color(0xFF234B78)
            val marker = Color(0xFFF0A34A)
            val stroke = 3.dp.toPx()
            if (straight) {
                val cx = size.width * .5f
                val cy = size.height * .78f
                drawLine(line, Offset(size.width * .12f, cy), Offset(size.width * .88f, cy), stroke, cap = StrokeCap.Round)
                drawArc(
                    marker,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - 24.dp.toPx(), cy - 24.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(48.dp.toPx(), 48.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
                drawCircle(line, radius = 4.dp.toPx(), center = Offset(cx, cy))
            } else {
                val vertex = Offset(size.width * .24f, size.height * .79f)
                drawLine(line, vertex, Offset(size.width * .88f, vertex.y), stroke, cap = StrokeCap.Round)
                drawLine(line, vertex, Offset(vertex.x, size.height * .12f), stroke, cap = StrokeCap.Round)
                val markerSize = 12.dp.toPx()
                val square = Path().apply {
                    moveTo(vertex.x + markerSize, vertex.y)
                    lineTo(vertex.x + markerSize, vertex.y - markerSize)
                    lineTo(vertex.x, vertex.y - markerSize)
                }
                drawPath(square, marker, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(line, radius = 4.dp.toPx(), center = vertex)
            }
        }
        Text(value, Modifier.padding(top = 1.dp), color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, maxLines = 1)
    }
}

private data class LessonCopy(
    val choiceCorrect: String,
    val choiceIncorrect: String,
    val inputCorrect: String,
    val inputIncorrect: String,
    val matchingCorrect: String,
    val matchingIncorrect: String,
    val wrongAnglePair: String,
    val angleBuildCorrect: String,
    val angleBuildIncorrect: String,
    val checkAngle: String,
    val angleSliderLabel: String,
    val angleSliderInstruction: String,
    val stepIncorrect: String,
    val stepCorrect: String,
    val showHint: String,
    val continueAction: String,
    val degreeColumn: String,
    val angleColumn: String,
    val helpTip: String,
    val dismissHelpTip: String,
    val hintPrefix: String,
    val solutionTitle: String,
    val completedCorrect: String,
    val completedIncorrect: String,
    val finishLesson: String,
    val nextExercise: String,
    val submitAnswer: String,
    val submitStep: String,
    val submitMatching: String,
    val stepLabel: String,
    val answerPlaceholder: String,
    val calculator: String,
    val calculatorTitle: String,
    val calculatorClose: String,
    val calculatorClear: String,
    val calculatorResult: String,
    val calculatorError: String,
    val minus: String,
    val erase: String,
    val fractionNumerator: String,
    val fractionDenominator: String,
    val triangleReference: String,
    val triangleDescription: String,
)

private fun lessonCopy(spanish: Boolean) = if (spanish) LessonCopy(
    choiceCorrect = "¡Muy bien!",
    choiceIncorrect = "No es correcto. Inténtalo otra vez o revisa la explicación.",
    inputCorrect = "¡Muy bien!",
    inputIncorrect = "Revisa la pista e inténtalo otra vez.",
    matchingCorrect = "¡Muy bien, todas las parejas coinciden!",
    matchingIncorrect = "Aún hay parejas que corregir.",
    wrongAnglePair = "No corresponde. Elige de nuevo.",
    angleBuildCorrect = "¡Muy bien! Formaste un ángulo recto de %1\$d°.",
    angleBuildIncorrect = "Elegiste %1\$d°. Un ángulo recto mide %2\$d°. Ajusta el brazo e inténtalo otra vez.",
    checkAngle = "Comprobar",
    angleSliderLabel = "Ángulo en grados",
    angleSliderInstruction = "Desliza abajo para girar el segundo rayo y ver cómo cambia el ángulo.",
    stepIncorrect = "Revisa la explicación e inténtalo otra vez.",
    stepCorrect = "¡Bien! Ahora sigue con el siguiente paso.",
    showHint = "Ver pista",
    continueAction = "Continuar",
    degreeColumn = "Medida",
    angleColumn = "Dibujo",
    helpTip = "Toca la bombilla para ver la ayuda.",
    dismissHelpTip = "Cerrar sugerencia",
    hintPrefix = "Pista",
    solutionTitle = "Explicación",
    completedCorrect = "Ejercicio completado correctamente.",
    completedIncorrect = "Ejercicio revisado con la explicación.",
    finishLesson = "Terminar lección",
    nextExercise = "Siguiente ejercicio",
    submitAnswer = "Comprobar respuesta",
    submitStep = "Comprobar paso",
    submitMatching = "Comprobar parejas",
    stepLabel = "Paso",
    answerPlaceholder = "Respuesta",
    calculator = "Abrir calculadora",
    calculatorTitle = "Calculadora",
    calculatorClose = "Cerrar calculadora",
    calculatorClear = "Borrar todo",
    calculatorResult = "Resultado",
    calculatorError = "Revisa la operación",
    minus = "Signo menos",
    erase = "Borrar el último dígito",
    fractionNumerator = "Arriba · numerador",
    fractionDenominator = "Abajo · denominador",
    triangleReference = "Triángulo de referencia 3–4–5",
    triangleDescription = "Triángulo rectángulo de referencia: cateto opuesto 3, cateto adyacente 4, hipotenusa 5, ángulo alfa y ángulo recto.",
) else LessonCopy(
    choiceCorrect = "Iporã, jajapo porã!",
    choiceIncorrect = "Ndaha'éi upéva. Eñeha'ãjey térã ehecha pe ñemyesakã.",
    inputCorrect = "Iporã, jajapo porã!",
    inputIncorrect = "Ejesareko pe pista rehe ha eñeha'ãjey.",
    matchingCorrect = "Iporã, embojoaju porã!",
    matchingIncorrect = "Oĩ gueteri joaju tekotevẽva oñemyatyrõ.",
    wrongAnglePair = "Ndohéi. Eñeha'ãjey.",
    angleBuildCorrect = "Iporã! Ejapo peteĩ ángulo recto %1\$d° reheve.",
    angleBuildIncorrect = "Eiporavo %1\$d°. Pe ángulo recto oguereko %2\$d°. Emohenda pe brazo ha eñeha'ãjey.",
    checkAngle = "Ehecha",
    angleSliderLabel = "Ángulo medida",
    angleSliderInstruction = "Emomýi iguýpe pe control embojere hag̃ua pe rayo ha rehecha pe ángulo.",
    stepIncorrect = "Ejesareko pe ñemyesakã rehe ha eñeha'ãjey.",
    stepCorrect = "Iporã. Ko'ág̃a jaha pe ambue paso-pe.",
    showHint = "Ahecha pista",
    continueAction = "Eseguí",
    degreeColumn = "Papapy",
    angleColumn = "Ta'anga",
    helpTip = "Eikutu pe bombilla: ñemyesakã.",
    dismissHelpTip = "Emboty ko je'e",
    hintPrefix = "Pista",
    solutionTitle = "Ñemyesakã",
    completedCorrect = "Ko jejapo oiko porãma.",
    completedIncorrect = "Ko jejapo ojehechama ñemyesakã ndive.",
    finishLesson = "Amohu'ã mbo'epy",
    nextExercise = "Ahasa pe ambue jejapo-pe",
    submitAnswer = "Ahecha mbohovái",
    submitStep = "Ahecha paso",
    submitMatching = "Ahecha joaju",
    stepLabel = "Pehẽ",
    answerPlaceholder = "Mbohovái",
    calculator = "Eipe'a kalkuladóra",
    calculatorTitle = "Kalkuladóra",
    calculatorClose = "Emboty kalkuladóra",
    calculatorClear = "Emopotĩ opa mba'e",
    calculatorResult = "Osẽva",
    calculatorError = "Ejesareko pe operación rehe",
    minus = "Signo menos",
    erase = "Eipe'a pe papapy paha",
    fractionNumerator = "Yvate · numerador",
    fractionDenominator = "Iguýpe · denominador",
    triangleReference = "Triángulo techaukarã 3–4–5",
    triangleDescription = "Triángulo rectángulo rehegua: cateto opuesto 3, cateto adyacente 4, hipotenusa 5, ángulo alfa ha ángulo recto.",
)

@Composable
private fun ScreenHeader(
    title: String,
    spanish: Boolean,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onBack,
            modifier = Modifier.size(44.dp).clearAndSetSemantics {
                contentDescription = if (spanish) "Volver al mapa" else "Ejevy mapa-pe"
                role = Role.Button
                onClick(label = if (spanish) "Volver al mapa" else "Ejevy mapa-pe") { onBack(); true }
            },
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Ink),
        ) {
            Canvas(Modifier.size(22.dp)) {
                val stroke = 2.6.dp.toPx()
                val tip = Offset(size.width * .28f, size.height * .5f)
                drawLine(Ink, tip, Offset(size.width * .68f, size.height * .5f), stroke, cap = StrokeCap.Round)
                drawLine(Ink, tip, Offset(size.width * .49f, size.height * .29f), stroke, cap = StrokeCap.Round)
                drawLine(Ink, tip, Offset(size.width * .49f, size.height * .71f), stroke, cap = StrokeCap.Round)
            }
        }
        Text(
            title,
            Modifier.weight(1f).padding(start = 12.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Button(
            onClick = onToggleLanguage,
            modifier = Modifier.padding(start = 8.dp).height(40.dp).clearAndSetSemantics {
                contentDescription = if (spanish) "Volver a guaraní" else "Cambiar a español"
                role = Role.Button
                onClick(label = if (spanish) "Volver a guaraní" else "Cambiar a español") { onToggleLanguage(); true }
            },
            shape = RoundedCornerShape(13.dp),
            contentPadding = PaddingValues(horizontal = 13.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE4F3FF), contentColor = Accent),
        ) { Text(if (spanish) "GN" else "ES", fontWeight = FontWeight.ExtraBold) }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun ExercisePanel(
    lesson: Lesson,
    exercise: Exercise,
    spanish: Boolean,
    index: Int,
    total: Int,
    progress: ExerciseResult?,
    repository: ProgressRepository,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val copy = lessonCopy(spanish)
    var feedback by remember(exercise.id) { mutableStateOf<String?>(null) }
    var feedbackIsError by remember(exercise.id) { mutableStateOf(false) }
    LaunchedEffect(spanish) {
        feedback = null
        feedbackIsError = false
    }
    var helpOpen by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var calculatorOpen by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var helpTipClosed by rememberSaveable { mutableStateOf(false) }
    val helpTipDismissed by repository.helpTipDismissed.collectAsState(initial = true)
    val completed = progress?.isTerminal == true
    val angleMatching = (exercise as? MatchingExercise)?.id == "angles-match"
    val angleBuilder = (exercise as? ChoiceExercise)?.id == "angles-choice"
    val numericExercise = exercise is InputExercise || exercise is StepExercise
    var answer by rememberSaveable(exercise.id, progress?.answer) { mutableStateOf(progress?.answer.orEmpty()) }
    var denominatorSelected by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var choiceSelection by rememberSaveable(exercise.id) { mutableStateOf<String?>(null) }
    var angleDegrees by rememberSaveable(exercise.id) { mutableStateOf(45) }
    var angleChanged by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var angleSolved by rememberSaveable(exercise.id) { mutableStateOf(false) }
    val targetAngle = (exercise as? ChoiceExercise)?.correctOptionId?.toIntOrNull() ?: 90
    LaunchedEffect(exercise.id, progress?.answer) {
        if (angleBuilder && !angleChanged) {
            angleDegrees = progress?.answer?.toIntOrNull()?.coerceIn(0, 180) ?: 45
        }
    }
    val matchingSelections = remember(exercise.id, progress?.isTerminal, progress?.correct) {
        mutableStateMapOf<String, String>().apply {
            if (angleMatching && completed) putAll((exercise as MatchingExercise).pairs)
        }
    }
    val stepIndex = (exercise as? StepExercise)?.let {
        (progress?.stepIndex ?: 0).coerceIn(0, it.steps.lastIndex.coerceAtLeast(0))
    } ?: 0
    var stepAnswer by rememberSaveable(exercise.id, stepIndex) { mutableStateOf("") }
    var stepDenominatorSelected by rememberSaveable(exercise.id, stepIndex) { mutableStateOf(false) }
    val stepShape = (exercise as? StepExercise)?.steps?.getOrNull(stepIndex)?.let { numericAnswerShape(it.acceptedAnswers) }
    val scope = rememberCoroutineScope()
    val lastExercise = index == total - 1
    val dockSuccessMessage = feedback?.takeUnless { feedbackIsError }
        ?: if (completed && progress?.correct == true) copy.completedCorrect
        else if (angleSolved) copy.angleBuildCorrect.format(targetAngle)
        else null
    val openSolution: () -> Unit = {
        helpOpen = true
        if (!completed && progress?.solutionViewed != true) scope.launch { repository.showSolution(exercise.id) }
    }

    val submitNumericAnswer: () -> Unit = {
        val input = exercise as InputExercise
        val value = answerForValidation(answer, numericAnswerShape(input.acceptedAnswers))
        val correct = validateAnswer(input, value)
        feedback = if (correct) copy.inputCorrect else copy.inputIncorrect
        feedbackIsError = !correct
        scope.launch { repository.recordAnswer(input.id, value, correct) }
    }
    val submitChoice: () -> Unit = {
        val selected = choiceSelection
        if (exercise is ChoiceExercise && selected != null) {
            val correct = validateAnswer(exercise, selected)
            feedback = if (correct) copy.choiceCorrect else copy.choiceIncorrect
            feedbackIsError = !correct
            scope.launch { repository.recordAnswer(exercise.id, selected, correct) }
        }
    }
    val submitAngleBuilder: () -> Unit = {
        if (exercise is ChoiceExercise && angleBuilder && !completed && !angleSolved) {
            val selected = angleDegrees
            val correct = validateAnswer(exercise, selected.toString())
            angleChanged = true
            feedback = if (correct) copy.angleBuildCorrect.format(targetAngle) else copy.angleBuildIncorrect.format(selected, targetAngle)
            feedbackIsError = !correct
            if (correct) angleSolved = true
            scope.launch { repository.recordAnswer(exercise.id, selected.toString(), correct) }
        }
    }
    val submitMatching: () -> Unit = {
        if (exercise is MatchingExercise && matchingSelections.size == exercise.leftItems.size) {
            val selected = matchingSelections.toMap()
            val correct = validateMatching(exercise, selected)
            feedback = if (correct) copy.matchingCorrect else copy.matchingIncorrect
            feedbackIsError = !correct
            scope.launch { repository.recordAnswer(exercise.id, selected.toString(), correct) }
        }
    }
    val submitStep: () -> Unit = {
        if (exercise is StepExercise && stepShape != null && answerIsReady(stepAnswer, stepShape)) {
            val value = answerForValidation(stepAnswer, stepShape)
            val correct = validateStep(exercise, stepIndex, value)
            val nextIndex = stepIndex + 1
            feedback = when {
                !correct -> copy.stepIncorrect
                nextIndex == exercise.steps.size -> copy.inputCorrect
                else -> copy.stepCorrect
            }
            feedbackIsError = !correct
            if (correct) {
                stepAnswer = ""
                stepDenominatorSelected = false
            }
            scope.launch {
                if (correct && nextIndex < exercise.steps.size) repository.saveStepProgress(exercise.id, value, nextIndex)
                else repository.recordAnswer(exercise.id, value, correct, completedStepIndex = nextIndex.takeIf { correct })
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                LinearProgressIndicator(
                    progress = { (index + 1f) / total.coerceAtLeast(1) },
                    Modifier.fillMaxWidth(.78f).widthIn(max = 300.dp).height(7.dp),
                    color = Color(0xFF43B9E8),
                    trackColor = Color(0xFFE2ECF9),
                )
            }
            Card(
                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, Color(0xFFD4E3F5)),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text(
                        exercise.prompt,
                        style = if (angleBuilder) MaterialTheme.typography.headlineSmall
                        else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    if (lesson.id in ratioReferenceLessonIds) TriangleContextDiagram(copy)
                    when (exercise) {
                        is ChoiceExercise -> if (angleBuilder) {
                            AngleBuilderContent(
                                degrees = angleDegrees,
                                enabled = progress?.correct != true && !angleSolved,
                                instruction = copy.angleSliderInstruction,
                            )
                        } else ChoiceContent(exercise, completed, choiceSelection) { selected ->
                            choiceSelection = selected
                            feedback = null
                        }
                        is InputExercise -> NumericAnswerPad(
                            answer = answer,
                            shape = numericAnswerShape(exercise.acceptedAnswers),
                            denominatorSelected = denominatorSelected,
                            placeholder = copy.answerPlaceholder,
                            numeratorLabel = copy.fractionNumerator,
                            denominatorLabel = copy.fractionDenominator,
                            enabled = !completed,
                            onPartSelected = { denominatorSelected = it },
                        )
                        is MatchingExercise -> MatchingContent(
                            exercise,
                            completed,
                            matchingSelections,
                            spanish,
                            onSelectionChanged = { feedback = null },
                            onPairChecked = { correct, completePairs ->
                                feedbackIsError = !correct
                                feedback = when {
                                    !correct -> copy.wrongAnglePair
                                    completePairs != null -> copy.matchingCorrect
                                    else -> null
                                }
                                completePairs?.let { answer -> scope.launch { repository.recordAnswer(exercise.id, answer.toString(), true) } }
                            },
                        )
                        is StepExercise -> StepContent(exercise, progress, completed, answer = stepAnswer, denominatorSelected = stepDenominatorSelected,
                            onDenominatorSelected = { stepDenominatorSelected = it },
                            copy = copy)
                    }
                    val inlineFeedback = feedback?.takeIf { feedbackIsError }
                        ?: if (completed && progress?.correct != true) copy.completedIncorrect else null
                    inlineFeedback?.let { message ->
                        Surface(
                            color = if (feedbackIsError) Color(0xFFFFF1EA) else Color(0xFFF0F4FA),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(message, Modifier.fillMaxWidth().padding(12.dp), color = Ink, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
      }

        Column(
            Modifier.fillMaxWidth().background(Color(0xFFEAF3FF)).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (angleBuilder) {
                AngleSliderDock(
                    degrees = angleDegrees,
                    enabled = progress?.correct != true && !angleSolved,
                    copy = copy,
                    onDegreesChange = { degrees ->
                        angleDegrees = degrees
                        angleChanged = true
                        feedback = null
                        feedbackIsError = false
                    },
                )
            }
            if (exercise is InputExercise || exercise is StepExercise) {
                NumericKeypad(
                    enabled = !completed,
                    copy = copy,
                    onKey = { key ->
                        if (exercise is InputExercise) {
                            answer = editNumericAnswer(answer, key, numericAnswerShape(exercise.acceptedAnswers), denominatorSelected)
                        } else if (stepShape != null) {
                            stepAnswer = editNumericAnswer(stepAnswer, key, stepShape, stepDenominatorSelected)
                        }
                        feedback = null
                        feedbackIsError = false
                    },
                )
            }
            dockSuccessMessage?.let { message ->
                Surface(
                    Modifier.fillMaxWidth(),
                    color = Color(0xFFE8F5EC),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFB9DEC7)),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("✓", color = Success, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        Text(message, Modifier.weight(1f), color = Color(0xFF175C39), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                HelpActionButton(copy.showHint, onClick = openSolution)
                if (numericExercise) {
                    CalculatorActionButton(copy.calculator, onClick = { calculatorOpen = true }, modifier = Modifier.padding(start = 8.dp))
                }
                if (angleBuilder) {
                    val angleDone = completed || angleSolved
                    PrimaryAction(
                        text = if (angleDone) if (lastExercise) copy.finishLesson else copy.nextExercise else copy.checkAngle,
                        onClick = if (angleDone) {
                            { if (lastExercise) onFinish() else onNext() }
                        } else submitAngleBuilder,
                        modifier = Modifier.weight(1f).padding(start = if (numericExercise) 8.dp else 12.dp),
                    )
                } else if (angleMatching) {
                    PrimaryAction(
                        text = copy.continueAction,
                        onClick = onNext,
                        enabled = completed && matchingSelections.size == (exercise as MatchingExercise).leftItems.size,
                        modifier = Modifier.weight(1f).padding(start = if (numericExercise) 8.dp else 12.dp),
                    )
                } else if (completed) {
                    PrimaryAction(
                        text = if (lastExercise) copy.finishLesson else copy.nextExercise,
                        onClick = if (lastExercise) onFinish else onNext,
                        modifier = Modifier.weight(1f).padding(start = if (numericExercise) 8.dp else 12.dp),
                    )
                } else {
                    val action = when (exercise) {
                        is ChoiceExercise -> copy.submitAnswer to submitChoice
                        is InputExercise -> copy.submitAnswer to submitNumericAnswer
                        is MatchingExercise -> copy.submitMatching to submitMatching
                        is StepExercise -> copy.submitStep to submitStep
                    }
                    val ready = when (exercise) {
                        is ChoiceExercise -> choiceSelection != null
                        is InputExercise -> answerIsReady(answer, numericAnswerShape(exercise.acceptedAnswers))
                        is MatchingExercise -> matchingSelections.size == exercise.leftItems.size && exercise.leftItems.all { it.id in matchingSelections }
                        is StepExercise -> stepShape != null && answerIsReady(stepAnswer, stepShape)
                    }
                    PrimaryAction(
                        text = action.first,
                        onClick = action.second,
                        enabled = ready,
                        modifier = Modifier.weight(1f).padding(start = if (numericExercise) 8.dp else 12.dp),
                    )
                }
            }
        }
      }

      if (!helpTipDismissed && !helpTipClosed) {
          Surface(
              Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 76.dp).widthIn(max = 280.dp),
              color = Accent,
              shape = RoundedCornerShape(16.dp),
              shadowElevation = 8.dp,
          ) {
              Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                  Text(copy.helpTip, Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                  TextButton(
                      onClick = {
                          helpTipClosed = true
                          scope.launch { repository.dismissHelpTip() }
                      },
                      modifier = Modifier.clearAndSetSemantics {
                          contentDescription = copy.dismissHelpTip
                          role = Role.Button
                          onClick(label = copy.dismissHelpTip) {
                              helpTipClosed = true
                              scope.launch { repository.dismissHelpTip() }
                              true
                          }
                      },
                      contentPadding = PaddingValues(horizontal = 10.dp),
                  ) { Text("×", color = Color.White, style = MaterialTheme.typography.titleLarge) }
              }
          }
      }
    }

    if (helpOpen) ExerciseHelpDialog(lesson, exercise, spanish) { helpOpen = false }
    if (calculatorOpen) CalculatorDialog(copy, onDismiss = { calculatorOpen = false })
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
private val ratioReferenceLessonIds = setOf("ratios", "ratio-relations")

@Composable
private fun TriangleContextDiagram(copy: LessonCopy) {
    Surface(
        Modifier.fillMaxWidth().height(162.dp).semantics { contentDescription = copy.triangleDescription },
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF1F7FF),
        border = BorderStroke(1.dp, Color(0xFFD4E3F5)),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
            val width = maxWidth
            val height = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                val scale = min(size.width * .62f / 4f, size.height * .72f / 3f)
                val base = scale * 4f
                val altitude = scale * 3f
                val left = (size.width - base) / 2f
                val a = Offset(left, size.height * .86f)
                val b = Offset(left + base, a.y)
                val c = Offset(b.x, a.y - altitude)
                val shape = Path().apply {
                    moveTo(a.x, a.y)
                    lineTo(b.x, b.y)
                    lineTo(c.x, c.y)
                    close()
                }
                drawPath(shape, Color(0xFFDCEEFF))
                drawLine(Accent, a, b, 4.dp.toPx(), cap = StrokeCap.Round)
                drawLine(Accent, b, c, 4.dp.toPx(), cap = StrokeCap.Round)
                drawLine(Accent, c, a, 4.dp.toPx(), cap = StrokeCap.Round)

                val rightMark = scale * .28f
                val square = Path().apply {
                    moveTo(b.x - rightMark, b.y)
                    lineTo(b.x - rightMark, b.y - rightMark)
                    lineTo(b.x, b.y - rightMark)
                }
                drawPath(square, Color(0xFFF0A34A), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                val arcRadius = scale * .55f
                drawArc(
                    color = Color(0xFFF0A34A),
                    startAngle = 0f,
                    sweepAngle = -36.87f,
                    useCenter = false,
                    topLeft = Offset(a.x - arcRadius, a.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Text(
                copy.triangleReference,
                Modifier.align(Alignment.TopStart).background(Color.White.copy(alpha = .92f), RoundedCornerShape(10.dp)).padding(horizontal = 9.dp, vertical = 5.dp),
                color = Accent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("α", Modifier.offset(x = width * .32f, y = height * .60f), color = Color(0xFFB36A14), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text("3", Modifier.offset(x = width * .72f, y = height * .37f), color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text("4", Modifier.offset(x = width * .49f, y = height * .78f), color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text("5", Modifier.offset(x = width * .47f, y = height * .35f), color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#lesson-content
@Composable
private fun HelpActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(52.dp).clearAndSetSemantics {
            contentDescription = label
            role = Role.Button
            onClick(label = label) { onClick(); true }
        },
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFD4E3F5)),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Accent),
    ) {
        Image(painterResource(R.drawable.ic_lightbulb), contentDescription = null, modifier = Modifier.size(24.dp))
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun CalculatorActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.size(52.dp).clearAndSetSemantics {
            contentDescription = label
            role = Role.Button
            onClick(label = label) { onClick(); true }
        },
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFD4E3F5)),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Accent),
    ) {
        Canvas(Modifier.size(25.dp)) {
            val sw = 2.dp.toPx()
            drawRoundRect(Accent, topLeft = Offset(size.width * .16f, size.height * .08f), size = Size(size.width * .68f, size.height * .84f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = Stroke(sw))
            drawLine(Accent, Offset(size.width * .29f, size.height * .31f), Offset(size.width * .71f, size.height * .31f), sw, cap = StrokeCap.Round)
            listOf(.39f to .52f, .62f to .52f, .39f to .72f, .62f to .72f).forEach { (x, y) ->
                drawCircle(Accent, radius = 1.5.dp.toPx(), center = Offset(size.width * x, size.height * y))
            }
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun CalculatorDialog(copy: LessonCopy, onDismiss: () -> Unit) {
    var expression by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var hasError by rememberSaveable { mutableStateOf(false) }

    fun press(key: String) {
        hasError = false
        when (key) {
            "AC" -> { expression = ""; result = null }
            "⌫" -> { expression = expression.dropLast(1); result = null }
            "=" -> {
                result = runCatching { ArithmeticExpressionParser(expression).evaluate() }
                    .map { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }
                    .getOrNull()
                hasError = result == null
            }
            else -> {
                if (result != null && (key.firstOrNull()?.isDigit() == true || key == "." || key == "(")) expression = ""
                result = null
                expression = (expression + key).take(48)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Surface(
                Modifier.fillMaxWidth().widthIn(max = 440.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 14.dp,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(copy.calculatorTitle, Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = copy.calculatorClose
                                role = Role.Button
                                onClick(label = copy.calculatorClose) { onDismiss(); true }
                            },
                        ) { Text("×", color = Accent, style = MaterialTheme.typography.titleLarge) }
                    }
                    Surface(color = Color(0xFFF2F7FD), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.End) {
                            Text(expression.ifEmpty { "0" }, color = Muted, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(
                                if (hasError) copy.calculatorError else result ?: copy.calculatorResult,
                                color = if (hasError) Color(0xFFB34B36) else Ink,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                    }
                    val rows = listOf(
                        listOf("AC", "(", ")", "⌫"),
                        listOf("7", "8", "9", "÷"),
                        listOf("4", "5", "6", "×"),
                        listOf("1", "2", "3", "−"),
                        listOf("0", ".", "=", "+"),
                    )
                    rows.forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            row.forEach { key ->
                                val isAction = key in setOf("AC", "⌫", "=", "÷", "×", "−", "+")
                                Button(
                                    onClick = { press(key) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(13.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAction) Color(0xFFDCEEFF) else Color(0xFFF5FAFD),
                                        contentColor = if (isAction) Accent else Ink,
                                    ),
                                ) { Text(key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private class ArithmeticExpressionParser(private val source: String) {
    private var cursor = 0

    fun evaluate(): Double {
        require(source.isNotBlank())
        val value = parseExpression()
        skipSpaces()
        require(cursor == source.length && value.isFinite())
        return value
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            skipSpaces()
            value = when (source.getOrNull(cursor)) {
                '+' -> { cursor++; value + parseTerm() }
                '-', '−' -> { cursor++; value - parseTerm() }
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parseFactor()
        while (true) {
            skipSpaces()
            value = when (source.getOrNull(cursor)) {
                '*', '×' -> { cursor++; value * parseFactor() }
                '/', '÷' -> {
                    cursor++
                    val divisor = parseFactor()
                    require(divisor != 0.0)
                    value / divisor
                }
                else -> return value
            }
        }
    }

    private fun parseFactor(): Double {
        skipSpaces()
        when (source.getOrNull(cursor)) {
            '+' -> { cursor++; return parseFactor() }
            '-', '−' -> { cursor++; return -parseFactor() }
            '(' -> {
                cursor++
                val value = parseExpression()
                skipSpaces()
                require(source.getOrNull(cursor) == ')')
                cursor++
                return value
            }
        }
        val start = cursor
        var hasDigit = false
        var hasPoint = false
        while (true) {
            val character = source.getOrNull(cursor) ?: break
            when (character) {
                in '0'..'9' -> { hasDigit = true; cursor++ }
                '.' -> { require(!hasPoint); hasPoint = true; cursor++ }
                else -> break
            }
        }
        require(hasDigit)
        return source.substring(start, cursor).toDouble()
    }

    private fun skipSpaces() {
        while (source.getOrNull(cursor)?.isWhitespace() == true) cursor++
    }
}

@Composable
private fun ExerciseHelpDialog(lesson: Lesson, exercise: Exercise, spanish: Boolean, onDismiss: () -> Unit) {
    val copy = lessonCopy(spanish)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp).heightIn(max = 760.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 14.dp,
            ) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(copy.solutionTitle, Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onDismiss) { Text(if (spanish) "Cerrar" else "Mboty", color = Accent) }
                    }
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(if (spanish) "Objetivo" else "Ko mbo'epy", color = Accent, fontWeight = FontWeight.Bold)
                        Text(lesson.objective, color = Ink, style = MaterialTheme.typography.bodyMedium)
                        Text(if (spanish) "Teoría" else "Ñaikuaa", color = Accent, fontWeight = FontWeight.Bold)
                        if (lesson.id in ratioReferenceLessonIds) TriangleContextDiagram(copy)
                        lesson.theory.forEach { block ->
                            if (block.type == "formula") {
                                Surface(color = Color(0xFFF0F4FC), shape = RoundedCornerShape(14.dp)) {
                                    Text(block.body, Modifier.fillMaxWidth().padding(12.dp), color = Ink, fontWeight = FontWeight.SemiBold)
                                }
                            } else Text(block.body, color = Ink, style = MaterialTheme.typography.bodyMedium)
                            if (lesson.id == "angles" && "90°" in block.body && "180°" in block.body) AngleVisual()
                        }
                        Surface(color = Color(0xFFEAF3FF), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(copy.hintPrefix, color = Accent, fontWeight = FontWeight.Bold)
                                Text(exercise.hint, color = Ink, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Text(copy.solutionTitle, color = Accent, fontWeight = FontWeight.Bold)
                        exercise.solutionSteps.forEachIndexed { step, text ->
                            Text("${step + 1}. $text", color = Ink, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun ChoiceContent(exercise: ChoiceExercise, completed: Boolean, selectedId: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        exercise.options.forEach { option ->
            val selected = option.id == selectedId
            Button(
                onClick = { onSelect(option.id) },
                enabled = !completed,
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).then(
                    if (selected) Modifier.border(2.dp, Accent, RoundedCornerShape(18.dp)) else Modifier
                ).clearAndSetSemantics {
                    contentDescription = if (selected) "${option.text}, elegido" else option.text
                    role = Role.RadioButton
                    if (completed) disabled()
                    onClick(label = option.text) {
                        if (!completed) onSelect(option.id)
                        !completed
                    }
                },
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) Color(0xFFE4F3FF) else Color.White,
                    contentColor = Ink,
                    disabledContainerColor = Color(0xFFF0F4FA),
                    disabledContentColor = Muted,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 13.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (selected) "●" else "○", color = Accent, fontWeight = FontWeight.Bold)
                    Text(option.text, Modifier.padding(start = 10.dp), color = Ink)
                }
            }
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun AngleBuilderContent(
    degrees: Int,
    enabled: Boolean,
    instruction: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(instruction, Modifier.fillMaxWidth(), color = Muted, style = MaterialTheme.typography.bodyMedium)
        Surface(
            Modifier.fillMaxWidth(),
            color = Color(0xFFF2F7FD),
            shape = RoundedCornerShape(20.dp),
        ) {
            Canvas(Modifier.fillMaxWidth().height(232.dp).padding(horizontal = 8.dp, vertical = 6.dp)) {
                    val radius = min(size.width * .34f, size.height * .62f)
                    val vertex = Offset(size.width * .5f, size.height * .76f)
                    val baseEnd = Offset(vertex.x + radius, vertex.y)
                    val radians = -degrees * PI / 180.0
                    val movingEnd = Offset(
                        vertex.x + (radius * cos(radians)).toFloat(),
                        vertex.y + (radius * sin(radians)).toFloat(),
                    )
                    val arcRect = Rect(vertex.x - radius, vertex.y - radius, vertex.x + radius, vertex.y + radius)
                    val rayColor = if (enabled) Accent else Muted
                    val baselineColor = if (enabled) Color(0xFF657C99) else Muted
                    val sweepAngle = -degrees.toFloat()
                    val wedge = Path().apply {
                        moveTo(vertex.x, vertex.y)
                        lineTo(baseEnd.x, baseEnd.y)
                        arcTo(arcRect, startAngleDegrees = 0f, sweepAngleDegrees = sweepAngle, forceMoveTo = false)
                        close()
                    }
                    drawPath(wedge, Color(0xFF43B9E8).copy(alpha = if (enabled) .12f else .08f))
                    drawLine(baselineColor, vertex, baseEnd, strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(rayColor, vertex, movingEnd, strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = Color(0xFF43B9E8),
                        startAngle = 0f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(vertex.x - radius * .42f, vertex.y - radius * .42f),
                        size = Size(radius * .84f, radius * .84f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    )
                    if (degrees == 90) {
                        val mark = 18.dp.toPx()
                        drawLine(rayColor, Offset(vertex.x + mark, vertex.y), Offset(vertex.x + mark, vertex.y - mark), strokeWidth = 3.dp.toPx())
                        drawLine(rayColor, Offset(vertex.x + mark, vertex.y - mark), Offset(vertex.x, vertex.y - mark), strokeWidth = 3.dp.toPx())
                    }
                    drawCircle(Color(0xFFF0A34A), radius = 7.dp.toPx(), center = vertex)
        }
    }
}
}

@Composable
private fun AngleSliderDock(
    degrees: Int,
    enabled: Boolean,
    copy: LessonCopy,
    onDegreesChange: (Int) -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD7E6F7)),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(copy.angleSliderLabel, color = Muted, style = MaterialTheme.typography.labelLarge)
                Text("$degrees°", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            }
            Slider(
                value = degrees.toFloat(),
                onValueChange = { onDegreesChange(it.roundToInt().coerceIn(0, 180)) },
                enabled = enabled,
                valueRange = 0f..180f,
                steps = 179,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = copy.angleSliderLabel
                    stateDescription = "$degrees°"
                },
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0°", color = Muted, style = MaterialTheme.typography.labelSmall)
                Text("180°", color = Muted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private data class NumericAnswerShape(val fraction: Boolean, val root: Boolean)

private fun numericAnswerShape(answers: List<String>): NumericAnswerShape {
    val fraction = answers.firstOrNull { '/' in it }
    return NumericAnswerShape(
        fraction = fraction != null,
        root = fraction?.startsWith("√") == true || fraction?.startsWith("sqrt(") == true,
    )
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
private fun answerIsReady(answer: String, shape: NumericAnswerShape): Boolean {
    val value = answer.trim()
    if (value.isEmpty()) return false
    if (!shape.fraction) return true
    val parts = value.split('/', limit = 2)
    return parts.size == 2 && parts.all { it.isNotBlank() }
}

private fun answerForValidation(value: String, shape: NumericAnswerShape): String {
    if (!shape.fraction) return value
    val numerator = value.substringBefore('/')
    val denominator = value.substringAfter('/', "")
    return if (shape.root) "√$numerator/$denominator" else "$numerator/$denominator"
}

private fun editNumericAnswer(current: String, key: String, shape: NumericAnswerShape, denominatorSelected: Boolean): String {
    val slash = current.indexOf('/')
    val numerator = if (slash < 0) current else current.substring(0, slash)
    val denominator = if (slash < 0) "" else current.substring(slash + 1)
    fun editPart(part: String): String = when (key) {
        "⌫" -> part.dropLast(1)
        "−" -> if (part.startsWith('-')) part.drop(1) else "-$part"
        else -> part + key
    }
    if (!shape.fraction) return editPart(current)
    return if (denominatorSelected) "$numerator/${editPart(denominator)}" else "${editPart(numerator)}/$denominator".removeSuffix("/")
}

private val numericKeyRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("−", "0", "⌫"),
)

@Composable
private fun NumericAnswerPad(
    answer: String,
    shape: NumericAnswerShape,
    denominatorSelected: Boolean,
    placeholder: String,
    numeratorLabel: String,
    denominatorLabel: String,
    enabled: Boolean,
    onPartSelected: (Boolean) -> Unit,
) {
    val numerator = answer.substringBefore('/')
    val denominator = answer.substringAfter('/', "")
    Surface(
        Modifier.fillMaxWidth().height(if (shape.fraction) 104.dp else 62.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF5FAFD),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9E7F0)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (shape.fraction) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(numeratorLabel, color = Muted, style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (shape.root) Text("√", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.widthIn(min = 54.dp).height(28.dp).clip(RoundedCornerShape(7.dp))
                                    .background(if (!denominatorSelected) Color(0xFFD8EFFA) else Color.White)
                                    .border(if (!denominatorSelected) 1.5.dp else 1.dp, if (!denominatorSelected) Accent else Color(0xFFC7D6E7), RoundedCornerShape(7.dp))
                                    .clickable(enabled = enabled) { onPartSelected(false) }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(numerator.ifEmpty { "—" }, color = if (numerator.isEmpty()) Muted else Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Box(Modifier.widthIn(min = 60.dp).height(2.dp).background(Accent))
                            Box(
                                Modifier.widthIn(min = 54.dp).height(28.dp).clip(RoundedCornerShape(7.dp))
                                    .background(if (denominatorSelected) Color(0xFFD8EFFA) else Color.White)
                                    .border(if (denominatorSelected) 1.5.dp else 1.dp, if (denominatorSelected) Accent else Color(0xFFC7D6E7), RoundedCornerShape(7.dp))
                                    .clickable(enabled = enabled) { onPartSelected(true) }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(denominator.ifEmpty { "—" }, color = if (denominator.isEmpty()) Muted else Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Text(denominatorLabel, color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(
                    answer.ifEmpty { placeholder },
                    color = if (answer.isEmpty()) Color(0xFF8293A5) else Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun NumericKeypad(enabled: Boolean, copy: LessonCopy, onKey: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        numericKeyRows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { key ->
                    val accented = key == "−" || key == "⌫"
                    val keyDescription = when (key) {
                        "−" -> copy.minus
                        "⌫" -> copy.erase
                        else -> key
                    }
                    Button(
                        onClick = { onKey(key) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).height(50.dp)
                            .border(1.dp, if (accented) Color(0xFFB8D6E8) else Color(0xFFD8E3EC), RoundedCornerShape(14.dp))
                            .clearAndSetSemantics {
                                contentDescription = keyDescription
                                role = Role.Button
                                if (!enabled) disabled()
                                onClick(label = keyDescription) { if (enabled) onKey(key); enabled }
                            },
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (accented) Color(0xFFD8EAF5) else Color.White,
                            contentColor = if (accented) Color(0xFF28648F) else Ink,
                            disabledContainerColor = Color(0xFFE8EFF4),
                            disabledContentColor = Muted,
                        ),
                    ) { Text(key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun MatchingContent(
    exercise: MatchingExercise,
    completed: Boolean,
    selected: MutableMap<String, String>,
    spanish: Boolean,
    onSelectionChanged: () -> Unit,
    onPairChecked: (Boolean, Map<String, String>?) -> Unit,
) {
    val choosePair = if (spanish) "Elige una relación" else "Eiporavo peteĩ joaju"
    val showsAngleDiagram = exercise.leftItems.any { it.text.contains("°") } &&
        exercise.rightItems.any { it.id in setOf("acute", "right", "straight") }
    if (showsAngleDiagram) {
        AngleMatchingContent(exercise, completed, selected, spanish, onSelectionChanged, onPairChecked)
        return
    }
    exercise.leftItems.forEach { left ->
        var expanded by remember(left.id) { mutableStateOf(false) }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(left.text, Modifier.weight(.42f), color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Box(Modifier.weight(.58f)) {
                val current = exercise.rightItems.firstOrNull { it.id == selected[left.id] }?.text
                Button(
                    enabled = !completed,
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .border(if (current == null) 1.dp else 2.dp, if (current == null) Color(0xFFD0DCEB) else Accent, RoundedCornerShape(16.dp))
                        .clearAndSetSemantics {
                        contentDescription = "${left.text}. ${current ?: choosePair}"
                        role = Role.Button
                        if (completed) disabled()
                        onClick(label = choosePair) { if (!completed) expanded = true; !completed }
                    },
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (current == null) Color.White else Color(0xFFE4F3FF),
                        contentColor = Ink,
                        disabledContainerColor = Color(0xFFF0F4FA),
                        disabledContentColor = Muted,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(current ?: choosePair, Modifier.weight(1f), maxLines = 2, fontWeight = FontWeight.SemiBold)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    exercise.rightItems.forEach { right ->
                        DropdownMenuItem(
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = right.text
                                role = Role.Button
                                onClick(label = right.text) { selected[left.id] = right.id; expanded = false; onSelectionChanged(); true }
                            },
                            text = { Text(right.text) },
                            onClick = { selected[left.id] = right.id; expanded = false; onSelectionChanged() },
                        )
                    }
                }
            }
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun AngleMatchingContent(
    exercise: MatchingExercise,
    completed: Boolean,
    pairings: MutableMap<String, String>,
    spanish: Boolean,
    onSelectionChanged: () -> Unit,
    onPairChecked: (Boolean, Map<String, String>?) -> Unit,
) {
    val copy = lessonCopy(spanish)
    var activeLeft by remember(exercise.id) { mutableStateOf<String?>(null) }
    var activeRight by remember(exercise.id) { mutableStateOf<String?>(null) }

    fun pair(leftId: String, rightId: String) {
        activeLeft = null
        activeRight = null
        val correct = exercise.pairs[leftId] == rightId
        if (correct) pairings[leftId] = rightId
        onSelectionChanged()
        onPairChecked(correct, pairings.toMap().takeIf { correct && it.size == exercise.leftItems.size })
    }

    fun selectLeft(leftId: String) {
        if (completed || leftId in pairings) return
        val rightId = activeRight
        if (rightId == null) activeLeft = leftId else pair(leftId, rightId)
    }

    fun selectRight(rightId: String) {
        if (completed || rightId in pairings.values) return
        val leftId = activeLeft
        if (leftId == null) activeRight = rightId else pair(leftId, rightId)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(copy.degreeColumn, Modifier.weight(1f), color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(copy.angleColumn, Modifier.weight(1f), color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        exercise.leftItems.zip(exercise.rightItems).forEach { (left, right) ->
            val leftSolved = left.id in pairings
            val rightSolved = right.id in pairings.values
            val leftActive = activeLeft == left.id
            val rightActive = activeRight == right.id
            val leftEnabled = !completed && !leftSolved
            val rightEnabled = !completed && !rightSolved
            val solvedBackground = Color(0xFFE9EDF3)
            val idleBackground = Color(0xFFF4F7FB)
            val activeBackground = Color(0xFFE4F3FF)
            val solvedBorder = Color(0xFFD7DEE8)
            val idleBorder = Color(0xFFE2EAF3)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    Modifier.weight(1f).height(76.dp).clickable(enabled = leftEnabled) { selectLeft(left.id) }
                        .clearAndSetSemantics {
                        contentDescription = left.text
                        role = Role.Button
                        selected = leftActive || leftSolved
                        if (!leftEnabled) disabled()
                        onClick(label = left.text) {
                            if (leftEnabled) { selectLeft(left.id); true } else false
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = when { leftSolved -> solvedBackground; leftActive -> activeBackground; else -> idleBackground },
                    border = BorderStroke(if (leftActive) 2.dp else 1.dp, when { leftSolved -> solvedBorder; leftActive -> Accent; else -> idleBorder }),
                    shadowElevation = if (leftEnabled && !leftActive) 2.dp else if (leftActive) 4.dp else 0.dp,
                ) {
                    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Text(left.text, color = if (leftSolved) Muted else Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Box(Modifier.weight(1f).height(76.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(72.dp).shadow(if (rightEnabled && !rightActive) 2.dp else if (rightActive) 4.dp else 0.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                            .background(when { rightSolved -> solvedBackground; rightActive -> activeBackground; else -> idleBackground })
                            .border(if (rightActive) 2.dp else 1.dp, when { rightSolved -> solvedBorder; rightActive -> Accent; else -> idleBorder }, RoundedCornerShape(16.dp))
                            .clickable(enabled = rightEnabled) { selectRight(right.id) }
                            .clearAndSetSemantics {
                                contentDescription = right.text
                                role = Role.Button
                                selected = rightActive || rightSolved
                                if (!rightEnabled) disabled()
                                onClick(label = right.text) {
                                    if (rightEnabled) { selectRight(right.id); true } else false
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) { AngleRelationDiagram(right.id, Modifier.size(48.dp), muted = rightSolved) }
                }
            }
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun AngleRelationDiagram(relationId: String?, modifier: Modifier = Modifier, muted: Boolean = false) {
    Canvas(modifier) {
        val stroke = 2.2.dp.toPx()
        val color = if (muted) Muted else Accent
        val vertexColor = if (muted) Muted else Color(0xFFF0A34A)
        when (relationId) {
            "straight" -> {
                val vertex = Offset(size.width * .5f, size.height * .5f)
                drawLine(color, Offset(size.width * .1f, vertex.y), Offset(size.width * .9f, vertex.y), stroke, cap = StrokeCap.Round)
                drawCircle(vertexColor, radius = 3.dp.toPx(), center = vertex)
            }
            "right" -> {
                val vertex = Offset(size.width * .18f, size.height * .82f)
                drawLine(color, vertex, Offset(size.width * .9f, vertex.y), stroke, cap = StrokeCap.Round)
                drawLine(color, vertex, Offset(vertex.x, size.height * .1f), stroke, cap = StrokeCap.Round)
                val square = 8.dp.toPx()
                drawLine(color, Offset(vertex.x + square, vertex.y), Offset(vertex.x + square, vertex.y - square), stroke)
                drawLine(color, Offset(vertex.x + square, vertex.y - square), Offset(vertex.x, vertex.y - square), stroke)
                drawCircle(vertexColor, radius = 3.dp.toPx(), center = vertex)
            }
            else -> {
                val vertex = Offset(size.width * .18f, size.height * .82f)
                drawLine(color, vertex, Offset(size.width * .82f, vertex.y), stroke, cap = StrokeCap.Round)
                drawLine(color, vertex, Offset(size.width * .82f, size.height * .18f), stroke, cap = StrokeCap.Round)
                val radius = 9.dp.toPx()
                drawArc(
                    color,
                    startAngle = -45f,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = Offset(vertex.x - radius, vertex.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(stroke),
                )
                drawCircle(vertexColor, radius = 3.dp.toPx(), center = vertex)
            }
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun StepContent(
    exercise: StepExercise,
    progress: ExerciseResult?,
    completed: Boolean,
    answer: String,
    denominatorSelected: Boolean,
    onDenominatorSelected: (Boolean) -> Unit,
    copy: LessonCopy,
) {
    if (completed && progress?.correct == true) {
        exercise.steps.lastOrNull()?.let { lastStep ->
            NumericAnswerPad(
                answer = progress.answer.orEmpty(),
                shape = numericAnswerShape(lastStep.acceptedAnswers),
                denominatorSelected = false,
                placeholder = copy.answerPlaceholder,
                numeratorLabel = copy.fractionNumerator,
                denominatorLabel = copy.fractionDenominator,
                enabled = false,
                onPartSelected = {},
            )
        }
        exercise.steps.forEachIndexed { index, finishedStep ->
            Surface(color = Color(0xFFF0F4FC), shape = RoundedCornerShape(12.dp)) {
                Text("${index + 1}. ${finishedStep.explanation}", Modifier.fillMaxWidth().padding(11.dp), color = Ink, style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }
    val stepIndex = (progress?.stepIndex ?: 0).coerceIn(0, exercise.steps.lastIndex)
    val step = exercise.steps[stepIndex]
    val shape = numericAnswerShape(step.acceptedAnswers)
    Text("${copy.stepLabel} ${stepIndex + 1} / ${exercise.steps.size}", color = Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    exercise.steps.take(stepIndex).forEachIndexed { index, finishedStep ->
        Surface(color = Color(0xFFF0F4FC), shape = RoundedCornerShape(12.dp)) {
            Text("${index + 1}. ${finishedStep.explanation}", Modifier.fillMaxWidth().padding(11.dp), color = Ink, style = MaterialTheme.typography.bodySmall)
        }
    }
    Text(step.prompt, color = Ink, style = MaterialTheme.typography.bodyLarge)
    NumericAnswerPad(
        answer = answer,
        shape = shape,
        denominatorSelected = denominatorSelected,
        placeholder = copy.answerPlaceholder,
        numeratorLabel = copy.fractionNumerator,
        denominatorLabel = copy.fractionDenominator,
        enabled = !completed,
        onPartSelected = onDenominatorSelected,
    )
}

@Composable
private fun CompletionScreen(lesson: Lesson, progress: ProgressSnapshot, award: Int, onMap: () -> Unit) {
    val correct = lesson.exercises.count { progress.results[it.id]?.correct == true }
    val mastery = if (lesson.exercises.isEmpty()) 0 else correct * 100 / lesson.exercises.size
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(Modifier.size(92.dp), shape = CircleShape, color = Color(0xFFEAF5EE)) {
            Box(contentAlignment = Alignment.Center) { Text("✓", color = Accent, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold) }
        }
        Text("¡Oñemohu'ã!", Modifier.padding(top = 22.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(lesson.title, Modifier.padding(top = 6.dp), color = Muted, style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth().padding(top = 24.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceAround) {
                StatBlock("$correct/${lesson.exercises.size}", "Iporã", "Mbohovái")
                StatBlock("$mastery%", "Mba'ekuaa", "Ko mbo'epy")
                StatBlock("+$award", "XP", if (award > 0) "Pyhareve" else "Oñeme'ẽma")
            }
        }
        Text("Mbo'epy ambuéva ojepe'áma.", Modifier.padding(top = 18.dp), color = Muted, style = MaterialTheme.typography.bodyMedium)
        PrimaryAction("Ajevy tape-pe", onClick = onMap, modifier = Modifier.fillMaxWidth().padding(top = 18.dp))
    }
}

@Composable
private fun RecoveryScreen(loading: Boolean) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (loading) CircularProgressIndicator(color = Accent)
        Text(
            if (loading) "Ñahenonde hag̃ua…" else "Ndaikatúi ñamoñe'ẽ mbo'epy térã ñande progreso. Nde dato noñemoambúi. Eñepyrũjey aplicación.",
            Modifier.padding(top = 16.dp),
            color = Ink,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).clearAndSetSemantics {
            contentDescription = text
            role = Role.Button
            if (!enabled) disabled()
            onClick(label = text) { if (enabled) onClick(); enabled }
        },
        shape = RoundedCornerShape(18.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
        border = BorderStroke(1.dp, if (enabled) Color(0xFF174FAE) else Color(0xFFCDD8E6)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFD8E0EB),
            disabledContentColor = Color(0xFF78869B),
        ),
    ) { Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold) }
}

private val Accent = Color(0xFF2865C7)
private val Success = Color(0xFF237A4B)
private val Ink = Color(0xFF1E2B43)
private val Muted = Color(0xFF66728A)
private val MapBackground = Color(0xFFEAF2F7)
