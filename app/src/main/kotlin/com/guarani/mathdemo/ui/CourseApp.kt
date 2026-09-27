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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.guarani.mathdemo.course.TriangleDiagram
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
import com.guarani.mathdemo.tutor.OnlineTutor
import com.guarani.mathdemo.tutor.PendingTutorQuestion
import com.guarani.mathdemo.tutor.TutorConnection
import com.guarani.mathdemo.tutor.TutorHttpException
import com.guarani.mathdemo.tutor.TutorOutbox
import com.guarani.mathdemo.course.helpSteps
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.IOException
import java.math.BigDecimal

// @spec spec://modules/learning/FEAT-010-learning-demo#flow
@Composable
fun CourseApp(
    repository: ProgressRepository,
    courseAsset: String = CourseLoader.COURSE_ASSET,
    unlockAll: Boolean = false,
    startExerciseId: String? = null,
    previewReachedLessonId: String? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val course = remember { runCatching { CourseLoader.load(context, asset = courseAsset) }.getOrNull() }
    val spanishLessons = remember {
        runCatching { CourseLoader.load(context, "es", courseAsset).lessons.associateBy(Lesson::id) }.getOrDefault(emptyMap())
    }
    val progressState by repository.progress.collectAsState(initial = ProgressState.Loading)
    val previewIndex = course?.lessons?.indexOfFirst { it.id == previewReachedLessonId } ?: -1
    val progress = (progressState as? ProgressState.Ready)?.snapshot?.let { saved ->
        if (previewIndex < 0 || course == null) saved else {
            val completed = course.lessons.take(previewIndex)
            saved.copy(
                completedLessonIds = saved.completedLessonIds + completed.map(Lesson::id),
                completedBookIds = saved.completedBookIds + course.lessons.take(previewIndex + 1).map(Lesson::id),
                xp = saved.xp + completed.sumOf { it.xpReward },
            )
        }
    }
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val catalog = remember(context) { AvatarCatalog(context) }
    var tutorDraft by rememberSaveable { mutableStateOf<String?>(null) }
    var tutorSpanish by rememberSaveable { mutableStateOf(false) }
    fun selectTab(route: String) {
        if (route == "tutor") { tutorDraft = null; tutorSpanish = false }
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

    fun startExercises(lesson: Lesson, replaceBook: Boolean = false) {
        val firstExercise = progress.currentExerciseId
            ?.takeIf { progress.currentLessonId == lesson.id && lesson.exercises.any { it.id == progress.currentExerciseId } }
            ?: lesson.exercises.firstOrNull { progress.results[it.id]?.isTerminal != true }?.id
            ?: lesson.exercises.first().id
        scope.launch { repository.saveCursor(lesson.id, firstExercise) }
        nav.navigate("lesson/${lesson.id}") {
            if (replaceBook) popUpTo("book/{lessonId}") { inclusive = true }
        }
    }

    fun openBook(lesson: Lesson) { nav.navigate("book/${lesson.id}") }

    fun openLesson(lesson: Lesson) {
        if (!unlockAll && lesson.concept.isNotEmpty() &&
            lesson.id !in progress.completedBookIds && lesson.id !in progress.completedLessonIds
        ) openBook(lesson) else startExercises(lesson)
    }

    // Preview mode can jump straight to one exercise.
    LaunchedEffect(startExerciseId) {
        val lesson = course.lessons.firstOrNull { lesson -> lesson.exercises.any { it.id == startExerciseId } } ?: return@LaunchedEffect
        repository.saveCursor(lesson.id, startExerciseId!!)
        nav.navigate("lesson/${lesson.id}")
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      CompositionLocalProvider(LocalAvatarLook provides progress.avatar) {
        NavHost(navController = nav, startDestination = "map") {
            composable("map") {
                CourseMapScreen(
                    course = course,
                    progress = progress,
                    unlockAll = unlockAll,
                    selectedTab = "map",
                    onMap = { scope.launch { nav.popBackStack("map", inclusive = false) } },
                    onTutor = { selectTab("tutor") },
                    onProfile = { selectTab("profile") },
                    onOpen = ::openLesson,
                    onOpenBook = ::openBook,
                )
            }
            composable("tutor") {
                TutorScreen(
                    courseTitle = course.title,
                    selectedTab = "tutor",
                    onMap = { selectTab("map") },
                    onTutor = { selectTab("tutor") },
                    onProfile = { selectTab("profile") },
                    incomingDraft = tutorDraft,
                    spanishInitially = tutorSpanish,
                    onDraftConsumed = { tutorDraft = null },
                    onReturnToLesson = if (nav.previousBackStackEntry?.destination?.route == "lesson/{lessonId}")
                        ({ nav.popBackStack() }) else null,
                )
            }
            composable("profile") {
                NewProfileScreen(
                    course = course,
                    progress = progress,
                    catalog = catalog,
                    onOpen = ::openLesson,
                    onAvatar = { nav.navigate("avatar") },
                    onMap = { selectTab("map") },
                    onTutor = { selectTab("tutor") },
                    onProfile = { selectTab("profile") },
                )
            }
            composable("avatar") {
                AvatarEditorScreen(progress.avatar, progress, course, catalog,
                    onBack = { nav.popBackStack() },
                    onSave = { look -> scope.launch { repository.saveAvatar(look); nav.popBackStack() } })
            }
            composable(
                route = "book/{lessonId}",
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
            ) { entry ->
                val lesson = course.lessons.firstOrNull { it.id == entry.arguments?.getString("lessonId") }
                    ?: return@composable
                val finishBook: () -> Unit = {
                    scope.launch {
                        repository.completeBook(lesson.id)
                        startExercises(lesson, replaceBook = true)
                    }
                }
                if (lesson.concept.isNotEmpty()) {
                    ConceptScreen(lesson, spanishLessons[lesson.id], progress.bookReveal[lesson.id] ?: 0,
                        lesson.id in progress.completedBookIds || lesson.id in progress.completedLessonIds,
                        onBack = { nav.popBackStack() },
                        onReveal = { index -> scope.launch { repository.saveBookReveal(lesson.id, index) } },
                        onFinish = finishBook)
                } else BookScreen(lesson, spanishLessons[lesson.id], onBack = { nav.popBackStack() }, onContinue = finishBook)
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
                    onAskTutor = { question, spanish ->
                        tutorDraft = question
                        tutorSpanish = spanish
                        nav.navigate("tutor")
                    },
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
}

// @spec spec://modules/learning/FEAT-010-learning-demo#course-map
@Composable
private fun CourseMapScreen(
    course: Course,
    progress: ProgressSnapshot,
    unlockAll: Boolean,
    selectedTab: String,
    onMap: () -> Unit,
    onTutor: () -> Unit,
    onProfile: () -> Unit,
    onOpen: (Lesson) -> Unit,
    onOpenBook: (Lesson) -> Unit,
) {
    CourseMap3DScreen(course, progress, onOpen, onOpenBook, unlockAll) {
        CourseBottomBar(selectedTab = selectedTab, onMap = onMap, onTutor = onTutor, onProfile = onProfile)
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#flow
@Composable
private fun BookScreen(lesson: Lesson, spanishLesson: Lesson?, onBack: () -> Unit, onContinue: () -> Unit) {
    var spanish by rememberSaveable(lesson.id) { mutableStateOf(false) }
    val copy = if (spanish) spanishLesson ?: lesson else lesson
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding()) {
        ScreenHeader(copy.title, spanish, { spanish = !spanish }, onBack)
        LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Accent)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text(if (spanish) "Concepto" else "Ñemyesakã", color = Accent, fontWeight = FontWeight.Bold)
            Text(copy.objective, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
            Canvas(Modifier.fillMaxWidth().height(220.dp).padding(vertical = 24.dp).semantics {
                contentDescription = if (spanish) "Triángulo rectángulo con ángulo de 90 grados" else "Triángulo rectángulo oguerekóva ángulo 90 grado"
            }) {
                val a = Offset(size.width * .12f, size.height * .84f)
                val b = Offset(size.width * .88f, size.height * .84f)
                val c = Offset(size.width * .88f, size.height * .13f)
                val stroke = 4.dp.toPx()
                drawLine(Accent, a, b, stroke, cap = StrokeCap.Round)
                drawLine(Accent, b, c, stroke, cap = StrokeCap.Round)
                drawLine(Color(0xFFEEA44B), a, c, stroke, cap = StrokeCap.Round)
                val m = 18.dp.toPx()
                drawLine(Color(0xFFEEA44B), Offset(b.x - m, b.y), Offset(b.x - m, b.y - m), 3.dp.toPx())
                drawLine(Color(0xFFEEA44B), Offset(b.x - m, b.y - m), Offset(b.x, b.y - m), 3.dp.toPx())
            }
            copy.theory.forEach { block ->
                Text(block.body, Modifier.padding(bottom = 20.dp), style = MaterialTheme.typography.bodyLarge, color = Ink)
            }
        }
        Box(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp)) {
            PrimaryAction(if (spanish) "Ir a los ejercicios" else "Tembiaporãme", onContinue)
        }
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
    incomingDraft: String?,
    spanishInitially: Boolean,
    onDraftConsumed: () -> Unit,
    onReturnToLesson: (() -> Unit)?,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val outbox = remember(context) { TutorOutbox(context) }
    val client = remember { OnlineTutor() }
    var connection by remember { mutableStateOf(outbox.connection()) }
    var endpointDraft by rememberSaveable { mutableStateOf(connection.url) }
    var tokenDraft by rememberSaveable { mutableStateOf(connection.token) }
    var configuring by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf(outbox.pending()) }
    var statusText by remember { mutableStateOf("") }
    var retryAvailable by remember { mutableStateOf(false) }
    var spanish by rememberSaveable { mutableStateOf(spanishInitially) }
    fun t(gn: String, es: String) = if (spanish) es else gn
    val messages = remember {
        mutableStateListOf<TutorMessage>().apply {
            outbox.pending()?.let { last ->
                add(TutorMessage(last.question, fromTutor = false))
                last.answer?.let { add(TutorMessage(it, fromTutor = true)) }
            }
        }
    }
    val chatScroll = rememberScrollState()
    var draft by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(incomingDraft) {
        if (!incomingDraft.isNullOrBlank()) { draft = incomingDraft; onDraftConsumed() }
    }
    fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty() || !connection.configured || pending?.answer == null && pending != null) return
        val item = PendingTutorQuestion(question = message)
        if (!outbox.savePending(item)) {
            statusText = t("Ndaikatúi oñeñongatu porandu. Eha'ã jey.", "No se pudo guardar la pregunta. Intenta de nuevo.")
            return
        }
        pending = item
        messages += TutorMessage(message, fromTutor = false)
        retryAvailable = false
        statusText = t("Porandu oñeñongatúma. Oñemondo…", "Pregunta guardada. Enviando…")
        draft = ""
    }
    // @spec spec://modules/android/PROP-010-android-demo-architecture#online
    LaunchedEffect(pending?.id, connection) {
        var current = pending?.takeIf { it.answer == null } ?: return@LaunchedEffect
        if (!connection.configured) return@LaunchedEffect
        var waitMs = 3_000L
        while (currentCoroutineContext().isActive) {
            try {
                val result = if (current.accepted) client.status(connection, current.id)
                    else client.submit(connection, current)
                if (!current.accepted) {
                    current = current.copy(accepted = true)
                    outbox.savePending(current)
                    pending = current
                }
                when (result.status) {
                    "completed" -> {
                        val answer = result.answer.orEmpty()
                        if (answer.isBlank()) {
                            retryAvailable = true
                            statusText = t("Servidor nombohováiri. Ikatu reha'ã jey.", "El servidor no devolvió una respuesta. Puedes reintentar.")
                            break
                        }
                        val done = current.copy(answer = answer)
                        outbox.savePending(done)
                        pending = done
                        messages += TutorMessage(answer, fromTutor = true)
                        retryAvailable = false
                        statusText = t("Mbohovái internet rupive", "Respuesta Online")
                        break
                    }
                    "error", "not_found", "expired" -> {
                        retryAvailable = true
                        statusText = t("Ndaikatúi oñembohovái (${result.error ?: result.status}). Eha'ã jey.",
                            "No se pudo responder (${result.error ?: result.status}). Puedes reintentar.")
                        break
                    }
                    "queued", "submitting", "in_progress" -> {
                        retryAvailable = false
                        statusText = t("Oñeha'arõ mbohovái internet rupive…", "Esperando respuesta Online…")
                        delay(4_000)
                    }
                    else -> {
                        retryAvailable = true
                        statusText = t("Servidor mbohovái ndojekuaái. Eha'ã jey.", "Respuesta del servidor no reconocida. Puedes reintentar.")
                        break
                    }
                }
                waitMs = 3_000L
            } catch (error: TutorHttpException) {
                if (error.code == 422 && error.reason == "not_math") {
                    draft = current.question
                    outbox.clearPending()
                    pending = null
                    retryAvailable = false
                    statusText = t("Ko porandu ndaha'éi papapykuaa rehegua. Ehai jey.",
                        "Esta pregunta no se relaciona con las matemáticas. Reformúlala.")
                    break
                }
                if (error.code == 401 || error.code == 400 || error.code == 409 || error.code == 429 || error.code == 503) {
                    retryAvailable = error.code != 401 && error.code != 429
                    statusText = when (error.code) {
                        401 -> t("Jeike hag̃ua token ndoikói. Emohenda jeike.", "Token de acceso incorrecto. Configura la conexión.")
                        429 -> t("Opa ko jasy porandu renda. Eha'ã jey ambue jasy.", "Se alcanzó el límite mensual de preguntas. Intenta el próximo mes.")
                        else -> t("Servidor jejavy (${error.code}). Ejesareko ñemohenda rehe.",
                            "Error del servidor (${error.code}). Revisa la configuración.")
                    }
                    break
                }
                retryAvailable = false
                statusText = t("Internet ndopytái porã. Oñeha'ã jey…", "Conexión inestable. Reintentando…")
                delay(waitMs)
                waitMs = (waitMs * 2).coerceAtMost(30_000L)
            } catch (_: IOException) {
                retryAvailable = false
                statusText = t("Internet ndaipóri. Porandu oñeñongatúma; oñeha'ãta jey.",
                    "Sin conexión. La pregunta está guardada; se reintentará.")
                delay(waitMs)
                waitMs = (waitMs * 2).coerceAtMost(30_000L)
            }
        }
    }
    LaunchedEffect(messages.size) { chatScroll.animateScrollTo(chatScroll.maxValue) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF3F8FC)).statusBarsPadding().navigationBarsPadding().imePadding()) {
        StudyPageHeader(t("AI mbo'ehára", "Tutor"), courseTitle, "T",
            status = if (connection.configured) "ONLINE" else t("NE'ĨRA OÑEMOHENDA", "SIN CONFIGURAR"))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            if (onReturnToLesson != null) TextButton(onClick = onReturnToLesson) {
                Text(t("← Ejevy mbo'epy-pe", "← Volver a la lección"))
            } else Spacer(Modifier.width(1.dp))
            TextButton(onClick = { spanish = !spanish }) { Text(if (spanish) "GN" else "ES") }
        }
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 4.dp),
            shape = RoundedCornerShape(18.dp), color = Color.White,
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (connection.configured) t("Ne porandu oñemondo internet rupive AI-pe.",
                            "Las preguntas se envían por internet al proveedor de IA.")
                        else t("Emohenda servidor reporandu hag̃ua AI-pe.", "Configura tu servidor para preguntar a la IA."),
                        Modifier.weight(1f), color = Muted, style = MaterialTheme.typography.labelSmall,
                    )
                    TextButton(onClick = { configuring = !configuring }) { Text(t("Emohenda", "Configurar")) }
                }
                if (configuring) {
                    OutlinedTextField(
                        value = endpointDraft, onValueChange = { endpointDraft = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("URL de Cloudflare Worker") },
                    )
                    OutlinedTextField(
                        value = tokenDraft, onValueChange = { tokenDraft = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text(t("Jeike hag̃ua token", "Token de acceso")) },
                    )
                    TextButton(onClick = {
                        val next = TutorConnection(endpointDraft.trim(), tokenDraft.trim())
                        if (!next.configured) statusText = t("Ehai HTTPS URL ha token.", "Introduce una URL HTTPS y un token.")
                        else if (outbox.saveConnection(next)) {
                            connection = next
                            statusText = t("Jeike oñeñongatúma.", "Conexión guardada.")
                            configuring = false
                        } else statusText = t("Ndaikatúi oñeñongatu jeike.", "No se pudo guardar la conexión.")
                    }) { Text(t("Eñongatu", "Guardar")) }
                }
            }
        }
        OfflineModelCard(spanish)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 13.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(23.dp)).background(Color(0xFFEAF2F8))
                .verticalScroll(chatScroll).padding(horizontal = 13.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TutorBubble(TutorMessage(t("Maitei. Mba'épepa ikatu roipytyvõ ko mbo'epy rehe?",
                "Hola. ¿En qué te puedo ayudar con esta lección?"), fromTutor = true))
            TutorBubble(TutorMessage(t("Ikatu ñañe'ẽ seno, coseno ha ángulo recto rehe.",
                "Podemos hablar del seno, coseno y los ángulos rectos."), fromTutor = true))
            messages.forEach { message -> TutorBubble(message) }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Seno 30°", "Coseno 30°", "Ángulo recto").forEach { topic ->
                Surface(
                    Modifier.clickable(enabled = connection.configured && (pending == null || pending?.answer != null)) { send(topic) }, shape = RoundedCornerShape(18.dp), color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4E3EF)),
                ) {
                    Text(topic, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = Ink, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (statusText.isNotBlank()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(statusText, Modifier.weight(1f), color = Muted, style = MaterialTheme.typography.labelSmall)
                if (pending != null && pending?.answer == null && retryAvailable) {
                    TextButton(onClick = {
                        val retry = PendingTutorQuestion(question = pending!!.question)
                        if (outbox.savePending(retry)) {
                            pending = retry
                            retryAvailable = false
                            statusText = t("Oñemondo jey…", "Enviando de nuevo…")
                        }
                    }) { Text(t("Eha'ã jey", "Reintentar")) }
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
                placeholder = { Text(t("Ehai ne porandu...", "Escribe tu pregunta...")) },
                shape = RoundedCornerShape(23.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(draft) }),
            )
            Button(
                onClick = { send(draft) }, enabled = draft.isNotBlank() && connection.configured && (pending == null || pending?.answer != null),
                modifier = Modifier.padding(start = 8.dp).size(52.dp),
                shape = CircleShape, contentPadding = PaddingValues(0.dp),
            ) { Text("↑", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        }
        CourseBottomBar(selectedTab, onMap, onTutor, onProfile)
    }
}

// Offline Tutor model entry point; the download itself is not implemented yet.
// Draft canon: specs/modules/tutor/FEAT-011-ai-tutor.md (offline-provider).
@Composable
private fun OfflineModelCard(spanish: Boolean) {
    var requested by rememberSaveable { mutableStateOf(false) }
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp), color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4E3EF)),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (spanish) "Tutor sin internet" else "AI mbo'ehára internet'ỹre",
                    color = Ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (requested) { if (spanish) "La descarga todavía no está disponible." else "Ñemboguejy ndojeguerekói gueteri." }
                    else { if (spanish) "Modelo Gemma 3 1B · ≈550 MB · por Wi-Fi" else "Gemma 3 1B · ≈550 MB · Wi-Fi rupive" },
                    color = Muted, style = MaterialTheme.typography.labelSmall,
                )
            }
            TextButton(onClick = { requested = true }, enabled = !requested) {
                Text(if (spanish) "Descargar" else "Emboguejy", fontWeight = FontWeight.Bold)
            }
        }
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

// @spec spec://modules/learning/FEAT-010-learning-demo#lesson-content
@Composable
private fun LessonScreen(
    lesson: Lesson,
    spanishLesson: Lesson?,
    progress: ProgressSnapshot,
    repository: ProgressRepository,
    onBack: () -> Unit,
    onAskTutor: (String, Boolean) -> Unit,
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

    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
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
            completedCount = displayedLesson.exercises.count { progress.results[it.id]?.isTerminal == true },
            progress = progress.results[exercise.id],
            repository = repository,
            onAskTutor = {
                val question = if (spanish)
                    "Ayúdame a entender este ejercicio de trigonometría de ${displayedLesson.title}: ${exercise.prompt}"
                else
                    "Eipytyvõ chéve aikũmby hag̃ua ko trigonometría tembiaporã ${displayedLesson.title} rehegua: ${exercise.prompt}"
                onAskTutor(question, spanish)
            },
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
    val matchingLeftColumn: String,
    val matchingRightColumn: String,
    val matchingInstruction: String,
    val matchingSelected: String,
    val matchingPairLabel: String,
    val matchingSolutionReview: String,
    val matchingSolutionNotCounted: String,
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
    choiceIncorrect = "Todavía no es correcto. Puedes ver la explicación.",
    inputCorrect = "¡Muy bien!",
    inputIncorrect = "Todavía no es correcto. Puedes ver la explicación.",
    matchingCorrect = "¡Muy bien, todas las parejas coinciden!",
    matchingIncorrect = "Todavía no es correcto. Puedes ver la explicación.",
    wrongAnglePair = "Esta pareja no corresponde. Puedes ver la explicación.",
    angleBuildCorrect = "¡Muy bien! Formaste un ángulo recto de %1\$d°.",
    angleBuildIncorrect = "Elegiste %1\$d°. Un ángulo recto mide %2\$d°.",
    checkAngle = "Comprobar",
    angleSliderLabel = "Ángulo en grados",
    angleSliderInstruction = "Desliza abajo para girar el segundo rayo y ver cómo cambia el ángulo.",
    stepIncorrect = "Todavía no es correcto. Puedes ver la explicación.",
    stepCorrect = "¡Bien! Ahora sigue con el siguiente paso.",
    showHint = "Ver pista",
    continueAction = "Continuar",
    degreeColumn = "Medida",
    angleColumn = "Dibujo",
    matchingLeftColumn = "Columna izquierda",
    matchingRightColumn = "Columna derecha",
    matchingInstruction = "Elige una tarjeta de cada columna para formar una pareja.",
    matchingSelected = "Seleccionada",
    matchingPairLabel = "Pareja %1\$d",
    matchingSolutionReview = "Solución consultada",
    matchingSolutionNotCounted = "Estas parejas muestran la explicación y no cuentan como una respuesta correcta.",
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
    fractionNumerator = "Numerador",
    fractionDenominator = "Denominador",
    triangleReference = "Triángulo de referencia 3–4–5",
    triangleDescription = "Triángulo rectángulo de referencia: cateto opuesto 3, cateto adyacente 4, hipotenusa 5, ángulo alfa y ángulo recto.",
) else LessonCopy(
    choiceCorrect = "Iporã, jajapo porã!",
    choiceIncorrect = "Ndaha'éi upéva. Ehecha pe ñemyesakã.",
    inputCorrect = "Iporã, jajapo porã!",
    inputIncorrect = "Ndaha'éi upéva. Ehecha pe ñemyesakã.",
    matchingCorrect = "Iporã, embojoaju porã!",
    matchingIncorrect = "Ndaha'éi upéva. Ehecha pe ñemyesakã.",
    wrongAnglePair = "Ndohéi. Ehecha pe ñemyesakã.",
    angleBuildCorrect = "Iporã! Ejapo peteĩ ángulo recto %1\$d° reheve.",
    angleBuildIncorrect = "Eiporavo %1\$d°. Pe ángulo recto oguereko %2\$d°. Ehecha pe ñemyesakã.",
    checkAngle = "Ehecha",
    angleSliderLabel = "Ángulo medida",
    angleSliderInstruction = "Emomýi iguýpe pe control embojere hag̃ua pe rayo ha rehecha pe ángulo.",
    stepIncorrect = "Ehecha pe ñemyesakã.",
    stepCorrect = "Iporã. Ko'ág̃a jaha pe ambue paso-pe.",
    showHint = "Ahecha pista",
    continueAction = "Eseguí",
    degreeColumn = "Papapy",
    angleColumn = "Ta'anga",
    matchingLeftColumn = "Asúpe",
    matchingRightColumn = "Akatúa",
    matchingInstruction = "Eiporavo peteĩ tarheta peteĩteĩva gotyo embojoaju hag̃ua.",
    matchingSelected = "Eiporavopyre",
    matchingPairLabel = "Joaju %1\$d",
    matchingSolutionReview = "Ñemyesakã ojehechava'ekue",
    matchingSolutionNotCounted = "Ko'ã joaju ohechauka pe ñemyesakã ha noñecontái mbohovái oiko porãramo.",
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
    fractionNumerator = "Numerador",
    fractionDenominator = "Denominador",
    triangleReference = "Triángulo techaukarã 3–4–5",
    triangleDescription = "Triángulo rectángulo rehegua: cateto opuesto 3, cateto adyacente 4, hipotenusa 5, ángulo alfa ha ángulo recto.",
)

// @spec spec://modules/learning/FEAT-010-learning-demo#lesson-content
@Composable
internal fun ScreenHeader(
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
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Surface(Modifier.padding(top = 4.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFEAF4ED)) {
                Text(if (spanish) "Creado con el equipo Tume Arandu" else "Tume Arandu aty ndive ojejapo",
                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = Color(0xFF286B50),
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
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
    completedCount: Int,
    progress: ExerciseResult?,
    repository: ProgressRepository,
    onAskTutor: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val copy = lessonCopy(spanish)
    var feedback by remember(exercise.id) { mutableStateOf<String?>(null) }
    var feedbackIsError by remember(exercise.id) { mutableStateOf(false) }
    var resultSaving by remember(exercise.id) { mutableStateOf(false) }
    LaunchedEffect(spanish) {
        feedback = null
        feedbackIsError = false
    }
    var helpOpen by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var calculatorOpen by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var helpTipClosed by rememberSaveable { mutableStateOf(false) }
    val helpTipDismissed by repository.helpTipDismissed.collectAsState(initial = true)
    var fullSolutionFinished by rememberSaveable(exercise.id) { mutableStateOf(false) }
    val completed = progress?.correct == true || fullSolutionFinished
    val angleBuilder = exercise.visual == "angle_builder"
    val triangleChoice = exercise is ChoiceExercise && (exercise.visual == "triangle_choice" || exercise.triangle != null)
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
            if (exercise is MatchingExercise && progress?.correct == true) putAll(exercise.pairs)
        }
    }
    val stepIndex = (exercise as? StepExercise)?.let {
        (progress?.stepIndex ?: 0).coerceIn(0, it.steps.lastIndex.coerceAtLeast(0))
    } ?: 0
    var stepAnswer by rememberSaveable(exercise.id, stepIndex) { mutableStateOf("") }
    var stepAdvancePending by remember(exercise.id) { mutableStateOf(false) }
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
    }

    val submitNumericAnswer: () -> Unit = {
        val input = exercise as InputExercise
        val value = answerForValidation(answer, numericAnswerShape(input.acceptedAnswers))
        val correct = validateAnswer(input, value)
        feedback = if (correct) copy.inputCorrect else copy.inputIncorrect
        feedbackIsError = !correct
        resultSaving = true
        scope.launch { repository.recordAnswer(input.id, value, correct); resultSaving = false }
    }
    val submitChoice: () -> Unit = {
        val selected = choiceSelection
        if (exercise is ChoiceExercise && selected != null) {
            val correct = validateAnswer(exercise, selected)
            feedback = if (correct) copy.choiceCorrect else copy.choiceIncorrect
            feedbackIsError = !correct
            resultSaving = true
            scope.launch { repository.recordAnswer(exercise.id, selected, correct); resultSaving = false }
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
            resultSaving = true
            scope.launch { repository.recordAnswer(exercise.id, selected.toString(), correct); resultSaving = false }
        }
    }
    val submitMatching: () -> Unit = {
        if (exercise is MatchingExercise && matchingSelections.size == exercise.leftItems.size &&
            matchingSelections.values.toSet().size == matchingSelections.size
        ) {
            val selected = matchingSelections.toMap()
            val correct = validateMatching(exercise, selected)
            feedback = if (correct) copy.matchingCorrect else copy.matchingIncorrect
            feedbackIsError = !correct
            resultSaving = true
            scope.launch { repository.recordAnswer(exercise.id, selected.toString(), correct); resultSaving = false }
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
                stepAdvancePending = nextIndex < exercise.steps.size
                stepAnswer = ""
                stepDenominatorSelected = false
            }
            resultSaving = true
            scope.launch {
                if (correct && nextIndex < exercise.steps.size) repository.saveStepProgress(exercise.id, value, nextIndex)
                else repository.recordAnswer(exercise.id, value, correct, completedStepIndex = nextIndex.takeIf { correct })
                resultSaving = false
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
                    progress = { completedCount.toFloat() / total.coerceAtLeast(1) },
                    Modifier.fillMaxWidth(.78f).widthIn(max = 300.dp).height(7.dp),
                    color = Color(0xFF43B9E8),
                    trackColor = Color(0xFFE2ECF9),
                )
            }
            Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp)) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text(
                        exercise.prompt,
                        style = if (angleBuilder) MaterialTheme.typography.headlineSmall
                        else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    if (exercise.triangle != null || exercise.visual in setOf("triangle_choice", "fraction_triangle")) {
                        TriangleContextDiagram(copy, tall = triangleChoice, diagram = exercise.triangle)
                    }
                    if (exercise.visual == "straight_angle") StraightAngleDiagram()
                    when (exercise) {
                        is ChoiceExercise -> if (angleBuilder) {
                            AngleBuilderContent(
                                degrees = angleDegrees,
                                enabled = progress?.correct != true && !angleSolved,
                                instruction = copy.angleSliderInstruction,
                            )
                        } else if (!triangleChoice) ChoiceContent(exercise, completed || feedback != null, choiceSelection) { selected ->
                            choiceSelection = selected
                            feedback = null
                        }
                        is InputExercise -> {
                            val input: @Composable () -> Unit = {
                                NumericAnswerPad(
                                    answer = answer,
                                    shape = numericAnswerShape(exercise.acceptedAnswers),
                                    denominatorSelected = denominatorSelected,
                                    placeholder = copy.answerPlaceholder,
                                    numeratorLabel = copy.fractionNumerator,
                                    denominatorLabel = copy.fractionDenominator,
                                    enabled = !completed && feedback == null,
                                    onPartSelected = { denominatorSelected = it },
                                )
                            }
                            if (exercise.triangle == null && exercise.visual in setOf("standard_number", "standard_fraction")) {
                                Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) { input() }
                            } else input()
                        }
                        is MatchingExercise -> MatchingContent(
                            exercise,
                            completed || feedback != null,
                            matchingSelections,
                            fullSolutionFinished,
                            copy,
                            onSelectionChanged = { feedback = null },
                            onPairChecked = { correct, completePairs ->
                                feedbackIsError = !correct
                                feedback = when {
                                    !correct -> copy.wrongAnglePair
                                    completePairs != null -> copy.matchingCorrect
                                    else -> null
                                }
                                completePairs?.let { answer ->
                                    resultSaving = true
                                    scope.launch { repository.recordAnswer(exercise.id, answer.toString(), true); resultSaving = false }
                                }
                            },
                        )
                        is StepExercise -> StepContent(exercise, progress, completed, answer = stepAnswer, denominatorSelected = stepDenominatorSelected,
                            onDenominatorSelected = { stepDenominatorSelected = it },
                            copy = copy)
                    }
                }
            }
      }

        Column(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (angleBuilder && feedback == null && !completed) {
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
            if ((exercise is InputExercise || exercise is StepExercise) && feedback == null && !completed) {
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
            if (exercise is ChoiceExercise && triangleChoice && feedback == null && !completed) {
                ChoiceContent(exercise, completed, choiceSelection, stackedFractions = true) { selected ->
                    choiceSelection = selected
                    feedback = null
                }
            }
            val resultMessage = dockSuccessMessage ?: feedback?.takeIf { feedbackIsError }
            if (resultMessage != null && exercise is ChoiceExercise && triangleChoice) {
                val selectedOption = exercise.options.firstOrNull { it.id == choiceSelection }
                if (selectedOption != null) ResultChoice(selectedOption.text, !feedbackIsError)
            }
            if (resultMessage != null) {
                val correctResult = !feedbackIsError
                Column(
                    Modifier.fillMaxWidth().background(if (correctResult) Color(0xFFE9F7EE) else Color(0xFFFFEDE8), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(if (correctResult) "✓  $resultMessage" else "↻  $resultMessage",
                        color = if (correctResult) Color(0xFF207548) else Color(0xFFA64C3C),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = openSolution,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(1.dp, if (correctResult) Color(0xFF67B887) else Color(0xFFD9978B)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White,
                            contentColor = if (correctResult) Color(0xFF207548) else Color(0xFFA64C3C)),
                    ) { Text(copy.solutionTitle, fontWeight = FontWeight.Bold) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (feedback == null && !completed) HelpActionButton(copy.showHint, onClick = openSolution)
                if (numericExercise && feedback == null && !completed) {
                    CalculatorActionButton(copy.calculator, onClick = { calculatorOpen = true }, modifier = Modifier.padding(start = 8.dp))
                }
                if (feedback != null || completed) {
                    PrimaryAction(text = copy.continueAction, onClick = {
                        feedback = null
                        if (stepAdvancePending) stepAdvancePending = false else onNext()
                    }, enabled = !resultSaving, modifier = Modifier.weight(1f))
                } else if (angleBuilder) {
                    val angleDone = completed || angleSolved
                    PrimaryAction(
                        text = if (angleDone) if (lastExercise) copy.finishLesson else copy.nextExercise else copy.checkAngle,
                        onClick = if (angleDone) {
                            { if (lastExercise) onFinish() else onNext() }
                        } else submitAngleBuilder,
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
                        is MatchingExercise -> matchingSelections.size == exercise.leftItems.size &&
                            matchingSelections.values.toSet().size == matchingSelections.size &&
                            exercise.leftItems.all { it.id in matchingSelections }
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

    if (helpOpen) ExerciseHelpDialog(exercise, spanish, onDismiss = { helpOpen = false }, onAskTutor = {
        helpOpen = false
        onAskTutor()
    }, onFinish = {
        resultSaving = true
        scope.launch {
            repository.showSolution(exercise.id)
            fullSolutionFinished = true
            helpOpen = false
            resultSaving = false
        }
    })
    if (calculatorOpen) CalculatorDialog(copy, onDismiss = { calculatorOpen = false })
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises

@Composable
private fun TriangleContextDiagram(copy: LessonCopy, tall: Boolean = false, diagram: TriangleDiagram? = null) {
    Canvas(
        Modifier.fillMaxWidth().height(if (tall) 370.dp else 210.dp)
            .semantics { contentDescription = diagram?.let { "${it.angleLabel}: ${it.base}, ${it.opposite}, ${it.hypotenuse}" } ?: copy.triangleDescription },
    ) {
        val angle = diagram?.angleDegrees ?: 36.87
        val ratio = kotlin.math.tan(angle * PI / 180).toFloat()
        val widthFraction = if (diagram == null) { if (tall) .86f else .75f } else { if (tall) .76f else .68f }
        val heightFraction = if (diagram == null) { if (tall) .7f else .84f } else .72f
        val base = min(size.width * widthFraction, size.height * heightFraction / ratio)
        val altitude = base * ratio
        val scale = base / 4f
        val a = Offset((size.width - base) / 2f, (size.height + altitude) / 2f)
        val b = Offset(a.x + base, a.y)
        val c = Offset(b.x, a.y - altitude)
        val mark = scale * .32f
        val orange = Color(0xFFF0A34A)
        drawLine(orange, Offset(b.x - mark, b.y), Offset(b.x - mark, b.y - mark), 2.dp.toPx())
        drawLine(orange, Offset(b.x - mark, b.y - mark), Offset(b.x, b.y - mark), 2.dp.toPx())
        drawArc(orange, startAngle = -angle.toFloat(), sweepAngle = angle.toFloat(), useCenter = false,
            topLeft = Offset(a.x - scale * .45f, a.y - scale * .45f),
            size = Size(scale * .9f, scale * .9f), style = Stroke(2.dp.toPx()))
        val line = 4.dp.toPx()
        drawLine(Accent, a, b, line, cap = StrokeCap.Round)
        drawLine(Accent, b, c, line, cap = StrokeCap.Round)
        drawLine(Accent, c, a, line, cap = StrokeCap.Round)
        drawIntoCanvas { canvas ->
            val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                color = Ink.toArgb()
                textSize = 21.sp.toPx()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = NativePaint.Align.CENTER
            }
            canvas.nativeCanvas.drawText(diagram?.hypotenuse ?: "5", (a.x + c.x) / 2f - 12.dp.toPx(), (a.y + c.y) / 2f - 12.dp.toPx(), paint)
            canvas.nativeCanvas.drawText(diagram?.opposite ?: "3", b.x + 23.dp.toPx(), (b.y + c.y) / 2f + 6.dp.toPx(), paint)
            canvas.nativeCanvas.drawText(diagram?.base ?: "4", (a.x + b.x) / 2f, a.y + 27.dp.toPx(), paint)
            paint.color = Color(0xFFB36A14).toArgb()
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            // The angle label sits on the bisector, far enough that its box clears both sides and the arc;
            // when the triangle is too thin for that, it moves outside, left of the vertex.
            val label = diagram?.angleLabel ?: "α"
            val labelWidth = paint.measureText(label)
            val labelHeight = paint.textSize * .72f
            val clearance = kotlin.math.hypot(labelWidth, labelHeight) / 2f + line / 2f + 3.dp.toPx()
            val half = (angle * PI / 360).toFloat()
            val hypotenuse = kotlin.math.hypot(base, altitude)
            val inradius = (base + altitude - hypotenuse) / 2f
            val center = if (clearance <= inradius * .95f) {
                val distance = max(clearance / sin(half), scale * .45f + clearance)
                Offset(a.x + distance * cos(half), a.y - distance * sin(half))
            } else {
                Offset(a.x - labelWidth / 2f - 8.dp.toPx(), a.y - labelHeight / 2f)
            }
            canvas.nativeCanvas.drawText(label, center.x, center.y + labelHeight / 2f, paint)
        }
    }
}

@Composable
private fun StraightAngleDiagram() {
    Canvas(Modifier.fillMaxWidth().height(250.dp).semantics { contentDescription = "Ángulo llano: dos rayos sobre una línea recta" }) {
        val vertex = Offset(size.width / 2f, size.height * .58f)
        val radius = size.width * .28f
        drawArc(Color(0xFF43B9E8), 180f, 180f, false,
            Offset(vertex.x - radius, vertex.y - radius), Size(radius * 2, radius * 2),
            style = Stroke(3.dp.toPx()))
        drawLine(Accent, Offset(size.width * .1f, vertex.y), Offset(size.width * .9f, vertex.y), 4.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Color(0xFFF0A34A), 6.dp.toPx(), vertex)
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
            listOf(.41f to .52f, .59f to .52f, .41f to .70f, .59f to .70f).forEach { (x, y) ->
                drawCircle(Accent, radius = 1.2.dp.toPx(), center = Offset(size.width * x, size.height * y))
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
            "DEG" -> Unit
            "AC" -> { expression = ""; result = null }
            "⌫" -> { expression = expression.dropLast(1); result = null }
            "=" -> {
                result = runCatching { ArithmeticExpressionParser(expression).evaluate() }
                    .map { BigDecimal.valueOf(it).setScale(12, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() }
                    .getOrNull()
                hasError = result == null
            }
            else -> {
                if (result != null && (key.firstOrNull()?.isDigit() == true || key == "." || key == "(")) expression = ""
                result = null
                expression = (expression + when (key) { "sin", "cos", "tan" -> "$key("; "√" -> "sqrt("; "x²" -> "^2"; "xʸ" -> "^"; else -> key }).take(96)
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
                        listOf("sin", "cos", "tan", "√"),
                        listOf("π", "x²", "xʸ", "AC"),
                        listOf("(", ")", "DEG", "⌫"),
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

// @spec spec://modules/learning/FEAT-010-learning-demo#solutions
@Composable
private fun ExerciseHelpDialog(exercise: Exercise, spanish: Boolean, onDismiss: () -> Unit, onAskTutor: () -> Unit, onFinish: () -> Unit) {
    val copy = lessonCopy(spanish)
    // Keep the basic hint and the final answer even when the author supplied many steps.
    val steps = exercise.helpSteps()
    var step by rememberSaveable(exercise.id) { mutableStateOf(0) }
    val last = step == steps.lastIndex
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.fillMaxWidth().heightIn(max = 720.dp), shape = RoundedCornerShape(24.dp), color = Color.White) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(copy.solutionTitle, Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onDismiss) { Text(if (spanish) "Cerrar" else "Mboty") }
                    }
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${copy.stepLabel} ${step + 1} / ${steps.size}", color = Accent, fontWeight = FontWeight.Bold)
                        if (exercise.triangle != null || exercise.visual in setOf("triangle_choice", "fraction_triangle")) TriangleContextDiagram(copy, diagram = exercise.triangle)
                        Text(steps[step], color = Ink, style = MaterialTheme.typography.titleMedium)
                    }
                    if (last) Button(onClick = onAskTutor, modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, Accent),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F3FC), contentColor = Accent)) {
                        Text(if (spanish) "Preguntar al Tutor con IA" else "Eporandu AI mbo'ehárape", fontWeight = FontWeight.Bold)
                    }
                    PrimaryAction(text = if (last) copy.continueAction else if (spanish) "Siguiente" else "Esegi",
                        onClick = { if (last) onFinish() else step++ }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ResultChoice(text: String, correct: Boolean) {
    Surface(Modifier.fillMaxWidth().height(65.dp), shape = RoundedCornerShape(16.dp),
        color = if (correct) Color(0xFFE9F7EE) else Color(0xFFFFF3EF),
        border = BorderStroke(2.dp, if (correct) Color(0xFF67B887) else Color(0xFFD9978B))) {
        Box(contentAlignment = Alignment.Center) {
            val fraction = text.split('/', limit = 2)
            if (fraction.size == 2) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(fraction[0], color = Ink, fontWeight = FontWeight.Bold)
                Box(Modifier.width(34.dp).height(2.dp).background(Accent))
                Text(fraction[1], color = Ink, fontWeight = FontWeight.Bold)
            } else Text(text, color = Ink, fontWeight = FontWeight.Bold)
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun ChoiceContent(
    exercise: ChoiceExercise,
    completed: Boolean,
    selectedId: String?,
    stackedFractions: Boolean = false,
    onSelect: (String) -> Unit,
) {
    if (stackedFractions) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            exercise.options.forEach { option ->
                val selected = option.id == selectedId
                val fraction = option.text.split('/', limit = 2)
                Button(
                    onClick = { onSelect(option.id) },
                    enabled = !completed,
                    modifier = Modifier.fillMaxWidth().height(76.dp)
                        .border(if (selected) 2.dp else 1.dp, if (selected) Accent else Color(0xFF718299), RoundedCornerShape(16.dp))
                        .clearAndSetSemantics {
                            contentDescription = option.text
                            role = Role.RadioButton
                            this.selected = selected
                            if (completed) disabled()
                            onClick(label = option.text) { if (!completed) onSelect(option.id); !completed }
                        },
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 0.dp, disabledElevation = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFFE4F3FF) else Color.White,
                        contentColor = Ink,
                        disabledContainerColor = Color(0xFFF0F4FA),
                        disabledContentColor = Muted,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    if (fraction.size == 2) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Text(fraction[0], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Box(Modifier.width(34.dp).height(2.dp).background(if (selected) Accent else Color(0xFF7C91A8)))
                            Text(fraction[1], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        }
                    } else Text(option.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            exercise.options.forEach { option ->
                val selected = option.id == selectedId
                Button(
                    onClick = { onSelect(option.id) },
                    enabled = !completed,
                    modifier = Modifier.fillMaxWidth().heightIn(min = if (exercise.options.size <= 3) 110.dp else 82.dp).then(
                        Modifier.border(if (selected) 2.dp else 1.dp, if (selected) Accent else Color(0xFF718299), RoundedCornerShape(18.dp))
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
                    Text(option.text, Modifier.fillMaxWidth(), color = Ink,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
        Canvas(Modifier.fillMaxWidth().height(460.dp).padding(horizontal = 8.dp, vertical = 6.dp)) {
                    val radius = min(size.width * .42f, size.height * .62f)
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
            Text("$degrees°", Modifier.fillMaxWidth(), color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
    Box(
        Modifier.fillMaxWidth().height(if (shape.fraction) 152.dp else 70.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (shape.fraction) Modifier else Modifier.border(1.dp, Color(0xFFD4E3F5), RoundedCornerShape(16.dp))),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (shape.fraction) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(numeratorLabel, color = Muted, style = MaterialTheme.typography.labelSmall)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (shape.root) Text("√", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Box(
                                Modifier.widthIn(min = 84.dp).height(42.dp).clip(RoundedCornerShape(7.dp))
                                    .background(if (!denominatorSelected) Color(0xFFD8EFFA) else Color.White)
                                    .border(if (!denominatorSelected) 1.5.dp else 1.dp, if (!denominatorSelected) Accent else Color(0xFFC7D6E7), RoundedCornerShape(7.dp))
                                    .clickable(enabled = enabled) { onPartSelected(false) }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(numerator.ifEmpty { "—" }, color = if (numerator.isEmpty()) Muted else Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box(Modifier.width(if (shape.root) 110.dp else 94.dp).height(2.dp).background(Accent))
                            Box(
                                Modifier.widthIn(min = 84.dp).height(42.dp).clip(RoundedCornerShape(7.dp))
                                    .background(if (denominatorSelected) Color(0xFFD8EFFA) else Color.White)
                                    .border(if (denominatorSelected) 1.5.dp else 1.dp, if (denominatorSelected) Accent else Color(0xFFC7D6E7), RoundedCornerShape(7.dp))
                                    .clickable(enabled = enabled) { onPartSelected(true) }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(denominator.ifEmpty { "—" }, color = if (denominator.isEmpty()) Muted else Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
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
                        modifier = Modifier.weight(1f).height(56.dp)
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
    solutionViewed: Boolean,
    copy: LessonCopy,
    onSelectionChanged: () -> Unit,
    onPairChecked: (Boolean, Map<String, String>?) -> Unit,
) {
    val showsAngleDiagram = exercise.visual == "angle_pairs"
    if (showsAngleDiagram) {
        AngleMatchingContent(exercise, completed, solutionViewed, selected, copy, onSelectionChanged, onPairChecked)
        return
    }
    var activeLeft by remember(exercise.id) { mutableStateOf<String?>(null) }
    var activeRight by remember(exercise.id) { mutableStateOf<String?>(null) }
    val displayedPairs = if (solutionViewed) exercise.pairs else selected
    LaunchedEffect(solutionViewed) {
        if (solutionViewed) {
            activeLeft = null
            activeRight = null
        }
    }

    fun assignPair(leftId: String, rightId: String) {
        selected.entries.toList().filter { it.key != leftId && it.value == rightId }.forEach { selected.remove(it.key) }
        selected[leftId] = rightId
        activeLeft = null
        activeRight = null
        onSelectionChanged()
    }

    fun chooseLeft(leftId: String) {
        if (completed) return
        val rightId = activeRight
        if (rightId != null) assignPair(leftId, rightId)
        else {
            activeLeft = if (activeLeft == leftId) null else leftId
            activeRight = null
            onSelectionChanged()
        }
    }

    fun chooseRight(rightId: String) {
        if (completed) return
        val leftId = activeLeft
        if (leftId != null) assignPair(leftId, rightId)
        else {
            activeRight = if (activeRight == rightId) null else rightId
            activeLeft = null
            onSelectionChanged()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (solutionViewed) MatchingSolutionReview(copy)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                exercise.leftItems.forEachIndexed { index, left ->
                    val paired = left.id in displayedPairs
                    val pairNumber = if (paired) index + 1 else null
                    val active = activeLeft == left.id
                    val rightText = exercise.rightItems.firstOrNull { it.id == displayedPairs[left.id] }?.text
                    MatchingPairCard(
                        text = left.text,
                        description = buildString {
                            append("${copy.matchingLeftColumn}: ${left.text}")
                            if (pairNumber != null) append(". ${copy.matchingPairLabel.format(pairNumber)}: $rightText")
                            else if (active) append(". ${copy.matchingSelected}")
                        },
                        pairNumber = pairNumber,
                        paired = paired,
                        active = active,
                        enabled = !completed,
                        onClick = { chooseLeft(left.id) },
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                exercise.rightItems.forEach { right ->
                    val leftId = displayedPairs.entries.firstOrNull { it.value == right.id }?.key
                    val leftIndex = exercise.leftItems.indexOfFirst { it.id == leftId }
                    val pairNumber = (leftIndex + 1).takeIf { leftIndex >= 0 }
                    val active = activeRight == right.id
                    val leftText = exercise.leftItems.firstOrNull { it.id == leftId }?.text
                    MatchingPairCard(
                        text = right.text,
                        description = buildString {
                            append("${copy.matchingRightColumn}: ${right.text}")
                            if (pairNumber != null) append(". ${copy.matchingPairLabel.format(pairNumber)}: $leftText")
                            else if (active) append(". ${copy.matchingSelected}")
                        },
                        pairNumber = pairNumber,
                        paired = pairNumber != null,
                        active = active,
                        enabled = !completed,
                        onClick = { chooseRight(right.id) },
                    )
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
    solutionViewed: Boolean,
    pairings: MutableMap<String, String>,
    copy: LessonCopy,
    onSelectionChanged: () -> Unit,
    onPairChecked: (Boolean, Map<String, String>?) -> Unit,
) {
    var activeLeft by remember(exercise.id) { mutableStateOf<String?>(null) }
    var activeRight by remember(exercise.id) { mutableStateOf<String?>(null) }
    val displayedPairs = if (solutionViewed) exercise.pairs else pairings
    LaunchedEffect(solutionViewed) {
        if (solutionViewed) {
            activeLeft = null
            activeRight = null
        }
    }

    fun pair(leftId: String, rightId: String) {
        activeLeft = null
        activeRight = null
        val correct = exercise.pairs[leftId] == rightId
        if (correct) pairings[leftId] = rightId
        onSelectionChanged()
        onPairChecked(correct, pairings.toMap().takeIf { correct && it.size == exercise.leftItems.size })
    }

    fun selectLeft(leftId: String) {
        if (completed || leftId in displayedPairs) return
        val rightId = activeRight
        if (rightId == null) {
            activeLeft = if (activeLeft == leftId) null else leftId
            onSelectionChanged()
        } else pair(leftId, rightId)
    }

    fun selectRight(rightId: String) {
        if (completed || rightId in displayedPairs.values) return
        val leftId = activeLeft
        if (leftId == null) {
            activeRight = if (activeRight == rightId) null else rightId
            onSelectionChanged()
        } else pair(leftId, rightId)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (solutionViewed) MatchingSolutionReview(copy)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                exercise.leftItems.forEachIndexed { index, left ->
                    val paired = left.id in displayedPairs
                    val pairNumber = if (paired) index + 1 else null
                    val active = activeLeft == left.id
                    val rightText = exercise.rightItems.firstOrNull { it.id == displayedPairs[left.id] }?.text
                    MatchingPairCard(
                        text = left.text,
                        description = buildString {
                            append("${copy.degreeColumn}: ${left.text}")
                            if (pairNumber != null) append(". ${copy.matchingPairLabel.format(pairNumber)}: $rightText")
                            else if (active) append(". ${copy.matchingSelected}")
                        },
                        pairNumber = pairNumber,
                        paired = paired,
                        active = active,
                        enabled = !completed,
                        onClick = { selectLeft(left.id) },
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                exercise.rightItems.forEach { right ->
                    val leftId = displayedPairs.entries.firstOrNull { it.value == right.id }?.key
                    val leftIndex = exercise.leftItems.indexOfFirst { it.id == leftId }
                    val pairNumber = (leftIndex + 1).takeIf { leftIndex >= 0 }
                    val active = activeRight == right.id
                    val leftText = exercise.leftItems.firstOrNull { it.id == leftId }?.text
                    MatchingPairCard(
                        text = right.text,
                        description = buildString {
                            append("${copy.angleColumn}: ${right.text}")
                            if (pairNumber != null) append(". ${copy.matchingPairLabel.format(pairNumber)}: $leftText")
                            else if (active) append(". ${copy.matchingSelected}")
                        },
                        pairNumber = pairNumber,
                        paired = pairNumber != null,
                        active = active,
                        enabled = !completed,
                        relationId = right.diagram,
                        onClick = { selectRight(right.id) },
                    )
                }
            }
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun MatchingSolutionReview(copy: LessonCopy) {
    Surface(
        color = Color(0xFFFFF6E8),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFF1D2A5)),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(copy.matchingSolutionReview, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(copy.matchingSolutionNotCounted, color = Ink, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
@Composable
private fun MatchingPairCard(
    text: String,
    description: String,
    pairNumber: Int?,
    paired: Boolean,
    active: Boolean,
    enabled: Boolean,
    relationId: String? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val background = when {
        active -> Color(0xFFE4F3FF)
        paired -> Color(0xFFF0F6FC)
        else -> Color.White
    }
    val edge = if (active) Accent else if (paired) Color(0xFF96B8D2) else Color(0xFFD4E0EB)
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            .clickable(enabled = enabled, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                selected = paired || active
                if (!enabled) disabled()
                onClick(label = description) { if (enabled) { onClick(); true } else false }
            },
        shape = shape,
        color = background,
        border = BorderStroke(if (active) 2.dp else 1.dp, edge),
        shadowElevation = if (enabled && !active) 2.dp else if (active) 4.dp else 0.dp,
    ) {
        Box(Modifier.fillMaxSize().padding(10.dp)) {
            if (pairNumber != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(26.dp).clip(CircleShape).background(Color(0xFFD9EAF7)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(pairNumber.toString(), color = Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
            Column(
                Modifier.fillMaxSize().padding(top = if (pairNumber != null) 16.dp else 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (relationId != null) AngleRelationDiagram(relationId, Modifier.size(48.dp), muted = paired)
                if (relationId == null && '/' in text) {
                    val fraction = text.split('/', limit = 2)
                    Column(Modifier.widthIn(max = 78.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(fraction[0].trim(), color = Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        Box(Modifier.fillMaxWidth().height(2.dp).background(Ink))
                        Text(fraction[1].trim(), color = Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                } else Text(
                    text,
                    color = if (enabled || paired) Ink else Muted,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
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
    val stepIndex = if (completed && progress?.correct == true) exercise.steps.lastIndex
        else (progress?.stepIndex ?: 0).coerceIn(0, exercise.steps.lastIndex)
    val shape = numericAnswerShape(exercise.steps[stepIndex].acceptedAnswers)
    val input: @Composable () -> Unit = {
        NumericAnswerPad(
            answer = if (completed && progress?.correct == true) progress.answer.orEmpty().removePrefix(if (shape.root) "√" else "") else answer,
            shape = shape,
            denominatorSelected = denominatorSelected,
            placeholder = copy.answerPlaceholder,
            numeratorLabel = copy.fractionNumerator,
            denominatorLabel = copy.fractionDenominator,
            enabled = !completed,
            onPartSelected = onDenominatorSelected,
        )
    }
    Box(Modifier.fillMaxWidth().height(if (exercise.triangle == null) 350.dp else if (shape.fraction) 150.dp else 90.dp), contentAlignment = Alignment.Center) {
        if (exercise.visual == "radical_fraction") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("1", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Box(Modifier.width(58.dp).height(2.dp).background(Ink))
                    Text("√2", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Text("=", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Box(Modifier.width(154.dp)) { input() }
            }
        } else input()
    }
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
internal fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
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

internal val Accent = Color(0xFF2865C7)
private val Success = Color(0xFF237A4B)
internal val Ink = Color(0xFF1E2B43)
private val Muted = Color(0xFF66728A)
private val MapBackground = Color(0xFFEAF2F7)
