package com.guarani.mathdemo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guarani.mathdemo.R
import com.guarani.mathdemo.course.Course
import com.guarani.mathdemo.course.Lesson
import com.guarani.mathdemo.progress.AvatarLook
import com.guarani.mathdemo.progress.ProgressSnapshot
import java.time.LocalDate
import kotlinx.coroutines.launch

internal val Nunito = FontFamily(Font(R.font.nunito_semibold, FontWeight.SemiBold), Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold), Font(R.font.nunito_black, FontWeight.Black))
internal val ProfileInk = Color(0xFF1D2B4F)
internal val ProfileMuted = Color(0xFF5D6A82)
internal val ProfileBlue = Color(0xFF2D63D6)
private val BlueEdge = Color(0xFF1F4BA8)
private val Yellow = Color(0xFFF5C230)
private val YellowEdge = Color(0xFFD69F12)

internal object ProfileStrings {
    const val profile = "Perfil"
    const val name = "Arami"
    const val handle = "@arami · Oñepyrũ 2026"
    const val stage = "Etapa 1"
    const val league = "Liga Bronce"
    const val xpTotal = "XP total"
    const val streakDays = "Días racha"
    const val lessons = "Lecciones"
    const val tape = "TAPE"
    const val stageTitle = "Etapa 1 · Triángulos"
    const val closet = "ARMARIO"
    const val collection = "Tu colección"
    const val nextReward = "Próximo premio"
    const val personalize = "Personalizar avatar"
    const val week = "Esta semana"
    const val weekCount = "0 / 7 días"
    const val weekHint = "Completa una lección hoy y enciende tu racha."
    const val achievements = "Logros"
    const val seeAll = "Ver todos"
    const val save = "Guardar"
    const val expression = "Expresión"
    const val back = "Volver al perfil"
    const val random = "Combinación aleatoria"
    val badgeLabels = listOf("Primer paso","Lados","Seno","Racha 7","Sin errores","Etapa 1")
    fun continueLesson(number:Int) = "Segui lección $number"
    fun continueReview(number:Int) = "Segui repaso $number"
    fun unlockedCount(open:Int,total:Int) = "$open de $total desbloqueados"
    fun locked(chip:String) = "BLOQUEADO · $chip"
}

@Composable
private fun Label(text: String, size: Int, weight: FontWeight = FontWeight.Bold, color: Color = ProfileInk,
    modifier: Modifier = Modifier, align: TextAlign? = null, maxLines: Int = Int.MAX_VALUE, shadow: Shadow? = null) {
    Text(text, modifier, color = color, fontFamily = Nunito, fontSize = size.sp, fontWeight = weight,
        textAlign = align, maxLines = maxLines, overflow = TextOverflow.Ellipsis, lineHeight = (size * 1.2f).sp,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false),shadow=shadow))
}

private fun Modifier.solidEdge(radius: Dp, y: Dp, color: Color): Modifier = drawBehind {
    drawRoundRect(color, topLeft = Offset(0f, y.toPx()), size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()))
}

@Composable
private fun WhiteCard(radius: Dp = 28.dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().solidEdge(radius, 8.dp, Color(0x2E285A19)).background(Color.White, RoundedCornerShape(radius))) {
        content()
    }
}

@Composable
private fun ProfileAction(text: String, height: Dp, blue: Boolean, onClick: () -> Unit) {
    val bg = if (blue) ProfileBlue else Color.White
    val edge = if (blue) BlueEdge else Color(0xFFDFE5EE)
    Box(Modifier.fillMaxWidth().height(height).solidEdge(18.dp, if (blue) 5.dp else 4.dp, edge)
        .background(bg, RoundedCornerShape(18.dp))
        .then(if (blue) Modifier else Modifier.border(2.dp, edge, RoundedCornerShape(18.dp)))
        .clickable(onClick = onClick).semantics { role = Role.Button }, contentAlignment = Alignment.Center) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            if (!blue) SimpleIcon("M12 20h9 M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z",20.dp,ProfileBlue)
            Label(text, if (blue) 17 else 16, FontWeight.Black, if (blue) Color.White else ProfileBlue)
        }
    }
}

@Composable
private fun SimpleIcon(path: String, size: Dp, color: Color, stroke: Float = 2.2f, modifier: Modifier = Modifier, fill: Color? = null) {
    Canvas(modifier.size(size)) {
        val p = PathParser().parsePathString(path).toPath()
        val scale = this.size.width / 24f
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            fill?.let { drawPath(p,it) }
            drawPath(p, color, style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

private data class MeadowEllipse(val x:Float,val y:Float,val rx:Float,val ry:Float)
private data class MeadowMark(val x:Float,val y:Float,val width:Float,val height:Float)
private data class MeadowFlower(val mark:MeadowMark,val color:Color)
private val profileEllipses = listOf(MeadowEllipse(60f,210f,140f,70f),MeadowEllipse(330f,520f,150f,80f),
    MeadowEllipse(80f,860f,160f,90f),MeadowEllipse(320f,1180f,150f,80f),MeadowEllipse(90f,1420f,150f,70f))
private val profileGrass = listOf(MeadowMark(18f,30f,26f,17f),MeadowMark(120f,16f,22f,15f),
    MeadowMark(300f,38f,28f,18f),MeadowMark(352f,120f,22f,15f),MeadowMark(10f,150f,24f,16f),
    MeadowMark(36f,262f,28f,18f),MeadowMark(344f,240f,26f,17f),MeadowMark(8f,470f,24f,16f),
    MeadowMark(356f,430f,24f,16f),MeadowMark(20f,640f,28f,18f),MeadowMark(350f,690f,26f,17f),
    MeadowMark(6f,930f,24f,16f),MeadowMark(360f,900f,22f,15f),MeadowMark(14f,1130f,26f,17f),
    MeadowMark(352f,1100f,28f,18f),MeadowMark(6f,1300f,24f,16f),MeadowMark(358f,1320f,24f,16f),
    MeadowMark(170f,1368f,26f,17f),MeadowMark(40f,1400f,22f,15f),MeadowMark(300f,1398f,28f,18f))
private val profileFlowers = listOf(MeadowFlower(MeadowMark(84f,44f,9f,9f),Color.White),
    MeadowFlower(MeadowMark(96f,52f,7f,7f),Color.White),MeadowFlower(MeadowMark(268f,22f,9f,9f),Color(0xFFF39BBD)),
    MeadowFlower(MeadowMark(366f,84f,8f,8f),Color(0xFFB79CF0)),MeadowFlower(MeadowMark(12f,212f,8f,8f),Color(0xFFB79CF0)),
    MeadowFlower(MeadowMark(370f,300f,9f,9f),Color.White),MeadowFlower(MeadowMark(4f,560f,8f,8f),Color(0xFFF39BBD)),
    MeadowFlower(MeadowMark(372f,600f,8f,8f),Color.White),MeadowFlower(MeadowMark(4f,780f,9f,9f),Color.White),
    MeadowFlower(MeadowMark(374f,820f,8f,8f),Color(0xFFB79CF0)),MeadowFlower(MeadowMark(6f,1040f,8f,8f),Color(0xFFF39BBD)),
    MeadowFlower(MeadowMark(372f,1010f,9f,9f),Color.White),MeadowFlower(MeadowMark(372f,1240f,8f,8f),Color(0xFFF39BBD)),
    MeadowFlower(MeadowMark(8f,1230f,9f,9f),Color.White),MeadowFlower(MeadowMark(120f,1390f,9f,9f),Color.White),
    MeadowFlower(MeadowMark(132f,1398f,7f,7f),Color(0xFFF39BBD)),MeadowFlower(MeadowMark(250f,1380f,9f,9f),Color(0xFFB79CF0)))

@Composable
private fun Meadow(editor: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = size.width / 390f
        val ellipses = if (editor) listOf(MeadowEllipse(60f,170f,130f,70f),MeadowEllipse(340f,120f,120f,60f)) else profileEllipses
        val grass = if (editor) listOf(MeadowMark(22f,130f,26f,17f),MeadowMark(336f,200f,28f,18f),
            MeadowMark(44f,236f,24f,16f),MeadowMark(306f,118f,22f,15f),MeadowMark(10f,286f,22f,15f),
            MeadowMark(350f,288f,24f,16f)) else profileGrass
        val flowers = if (editor) listOf(MeadowFlower(MeadowMark(62f,202f,9f,9f),Color.White),
            MeadowFlower(MeadowMark(74f,210f,7f,7f),Color(0xFFF39BBD)),
            MeadowFlower(MeadowMark(340f,160f,9f,9f),Color(0xFFB79CF0)),
            MeadowFlower(MeadowMark(316f,262f,8f,8f),Color.White),
            MeadowFlower(MeadowMark(28f,100f,8f,8f),Color.White)) else profileFlowers
        fun p(x: Float) = x * unit
        ellipses.forEach { (x,y,rx,ry) ->
            drawOval(Color(0xFF70C051), Offset(p(x-rx),p(y-ry)),Size(p(rx*2),p(ry*2)))
        }
        val bladePaths = listOf("M11 16 C10 10 7 6 2 3 C8 5 12 9 13 16 Z", "M12 16 C12 9 13 5 16 0 C15 6 15 11 14.5 16 Z",
            "M13 16 C15 11 18 8 23 6 C19 9 17 12 16 16 Z")
        grass.forEach { mark ->
            val svgScale=minOf(mark.width/24f,mark.height/16f)
            withTransform({ translate(p(mark.x+(mark.width-24f*svgScale)/2f),p(mark.y+(mark.height-16f*svgScale)/2f));
                scale(unit*svgScale,unit*svgScale,pivot=Offset.Zero) }) {
                bladePaths.forEachIndexed { i,d -> drawPath(PathParser().parsePathString(d).toPath(), if (i==1) Color(0xFF5CAE41) else Color(0xFF4F9F37)) }
            }
        }
        flowers.forEach { (mark,c) ->
            val x=mark.x+mark.width/2f; val y=mark.y+mark.height/2f; val s=mark.width/10f
            listOf(0f to -2.4f,2.4f to 0f,0f to 2.4f,-2.4f to 0f).forEach { (dx,dy) ->
                drawCircle(c,p(2.2f*s),Offset(p(x+dx*s),p(y+dy*s)))
            }
            drawCircle(Color(0xFFF7D34A),p(1.6f*s),Offset(p(x),p(y)))
        }
    }
}

// @spec spec://modules/android/PROP-010-android-demo-architecture#navigation
@Composable
internal fun NewProfileScreen(course: Course, progress: ProgressSnapshot, catalog: AvatarCatalog,
    onOpen: (Lesson) -> Unit, onAvatar: () -> Unit, onAdaptive: () -> Unit,
    onMap: () -> Unit, onTutor: () -> Unit, onProfile: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF79C95A))) {
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Meadow(false, Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val titleShadow=with(LocalDensity.current) { 2.dp.toPx() }
                Label(ProfileStrings.profile, 28, FontWeight.Black, Color.White, Modifier.padding(start = 4.dp),
                    shadow=Shadow(Color(0x59285A19),Offset(0f,titleShadow),0f))
                Spacer(Modifier.height(38.dp))
                ProfileIdentity(course, progress, onAvatar)
                WhiteCard {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Label("Mbo'epy ndéve g̃uarã", 19, FontWeight.Black)
                        Label("Descubre cómo cambiarán tus lecciones cuando necesites más práctica.", 14, FontWeight.Bold, ProfileMuted)
                        ProfileAction("Ehecha / Ver cómo funciona", 50.dp, true, onAdaptive)
                    }
                }
                ProfileTape(course, progress, onOpen)
                ProfileCollection(course, progress, catalog, onAvatar)
                ProfileWeek()
                ProfileAchievements()
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
            CourseBottomBar("profile", onMap, onTutor, onProfile)
        }
    }
}

// @spec spec://modules/learning/FEAT-010-learning-demo#adaptive-placeholder
@Composable
internal fun AdaptiveLessonsPlaceholderScreen(onBack: () -> Unit) {
    var spanish by rememberSaveable { mutableStateOf(false) }
    var showTestNotice by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color(0xFF79C95A))) {
        Meadow(false, Modifier.matchParentSize())
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(Color.White, CircleShape)
                    .clickable(onClick = onBack).semantics { contentDescription = if (spanish) "Volver al perfil" else "Ejevy perfil-pe"; role = Role.Button },
                    contentAlignment = Alignment.Center) { Label("←", 25, FontWeight.Black, ProfileBlue) }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(56.dp).background(Color.White, RoundedCornerShape(18.dp))
                    .clickable { spanish = !spanish }.semantics { contentDescription = if (spanish) "Cambiar a guaraní" else "Cambiar a español"; role = Role.Button },
                    contentAlignment = Alignment.Center) { Label(if (spanish) "GN" else "ES", 17, FontWeight.Black, ProfileBlue) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Label(if (spanish) "Lecciones a tu ritmo" else "Mbo'epy nde ritmópe", 28, FontWeight.Black, Color.White)
                Label(if (spanish) "Así cambiará tu práctica" else "Péicha oñemoambuéta nde práctica", 16, FontWeight.Bold, Color.White)
            }
            WhiteCard {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.background(Color(0xFFFFF3CC), CircleShape).padding(horizontal = 12.dp, vertical = 7.dp)) {
                        Label(if (spanish) "EN PREPARACIÓN" else "OÑEMBOSAKO'I", 12, FontWeight.Black, Color(0xFF876000))
                    }
                    Column(Modifier.fillMaxWidth().background(Color(0xFFEDF4FF), RoundedCornerShape(20.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Label(if (spanish) "Dificultad adaptativa" else "Mbo'epy hasykue", 17, FontWeight.Black,
                                modifier = Modifier.weight(1f))
                            Label("6 / 10", 23, FontWeight.Black, ProfileBlue)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(10) { level ->
                                Box(Modifier.weight(1f).height(13.dp).background(
                                    if (level < 6) ProfileBlue else Color(0xFFD4DFEF), RoundedCornerShape(4.dp)))
                            }
                        }
                        Label(if (spanish) "Valor de ejemplo: aún no es tu resultado."
                            else "Techapyrã añónte: ndaha'éi ne resultado.", 13, FontWeight.Bold, ProfileMuted)
                    }
                    Box(Modifier.fillMaxWidth().height(52.dp).background(Yellow, RoundedCornerShape(17.dp))
                        .clickable { showTestNotice = true }.semantics { role = Role.Button },
                        contentAlignment = Alignment.Center) {
                        Label(if (spanish) "Hacer la prueba" else "Ehasa prueba", 17, FontWeight.Black, ProfileInk)
                    }
                    if (showTestNotice) {
                        Label(if (spanish) "La prueba todavía está en preparación."
                            else "Prueba oñembosako'i gueteri.", 14, FontWeight.Bold, ProfileMuted)
                    }
                    Label(if (spanish) "¿Qué tendrá en cuenta?" else "Mba'épa ohecháta?", 21, FontWeight.Black)
                    AdaptiveSignal("↻", if (spanish) "Errores que se repiten" else "Rejavy jey jey",
                        if (spanish) "Un mismo error aparece varias veces." else "Peteĩ mba'e rejavy heta jey.")
                    AdaptiveSignal("!", if (spanish) "Errores que te frenan" else "Jejavy nde jokóva",
                        if (spanish) "El error impide avanzar." else "Pe jejavy nderehejái eho tenonde.")
                    Label("↓", 26, FontWeight.Black, ProfileBlue, Modifier.align(Alignment.CenterHorizontally))
                    Column(Modifier.fillMaxWidth().background(ProfileBlue, RoundedCornerShape(22.dp)).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Label(if (spanish) "Tu siguiente práctica" else "Nde práctica oúva", 19, FontWeight.Black, Color.White)
                        Label(if (spanish) "Más ejemplos, un repaso breve o ejercicios extra."
                            else "Techapyrãve, jehechajey mbyky térã ejercicio hetave.",
                            15, FontWeight.Bold, Color.White)
                    }
                }
            }
            Label(if (spanish) "Por ahora no analizamos tus respuestas ni cambiamos tus lecciones."
                else "Ko'ág̃a ndorohechái ne mbohovái ha noñemoambuéi gueteri nde mbo'epy.",
                14, FontWeight.Bold, Color.White, Modifier.padding(horizontal = 5.dp))
        }
    }
}

@Composable
private fun AdaptiveSignal(symbol: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFFFF8E3), RoundedCornerShape(18.dp)).padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(Yellow, CircleShape), contentAlignment = Alignment.Center) {
            Label(symbol, 23, FontWeight.Black, ProfileInk)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Label(title, 16, FontWeight.Black)
            Label(detail, 13, FontWeight.Bold, ProfileMuted)
        }
    }
}

@Composable
private fun ProfileIdentity(course: Course, progress: ProgressSnapshot, onAvatar: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 0.dp)) {
        WhiteCard(30.dp) {
            Column(Modifier.fillMaxWidth().padding(start=20.dp,end=20.dp,top=70.dp,bottom=18.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Label(ProfileStrings.name,26,FontWeight.Black)
                Label(ProfileStrings.handle,15,FontWeight.Bold,ProfileMuted)
                Row(Modifier.padding(top=10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(ProfileStrings.stage,Color(0xFFE8F0FE),ProfileBlue,
                        "M3 20 L20 4 L20 20 Z M16 20 L16 16 L20 16")
                    Chip(ProfileStrings.league,Color(0xFFFFF5D6),Color(0xFF8A6200),
                        "M12 2.8l2.8 5.8 6.3.9-4.6 4.4 1.1 6.3L12 17.2 6.4 20.2l1.1-6.3-4.6-4.4 6.3-.9Z",Yellow)
                }
                Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    StatTile("xp", "${progress.xp}", ProfileStrings.xpTotal,Modifier.weight(1f))
                    StatTile("streak", "0", ProfileStrings.streakDays,Modifier.weight(1f)) // TODO: streak data
                    StatTile("lessons", "${course.lessons.filterIndexed { index, _ -> index % 2 == 0 }.count { it.id in progress.completedLessonIds }}/${(course.lessons.size + 1) / 2}", ProfileStrings.lessons,Modifier.weight(1f))
                }
            }
        }
        Box(Modifier.align(Alignment.TopCenter).offset(y=(-62).dp).size(124.dp)
            .solidEdge(62.dp,8.dp,Color(0xFFC9D2DF)).background(Color.White,CircleShape).padding(6.dp),contentAlignment=Alignment.Center) {
            AvatarView(progress.avatar,112.dp)
        }
        Box(Modifier.align(Alignment.TopCenter).offset(x=48.dp,y=22.dp).size(40.dp)
            .solidEdge(20.dp,3.dp,YellowEdge).background(Yellow,CircleShape)
            .border(4.dp,Color.White,CircleShape).clickable(onClick=onAvatar).semantics { contentDescription=ProfileStrings.personalize; role=Role.Button },
            contentAlignment=Alignment.Center) {
            SimpleIcon("M12 20h9 M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z",16.dp,ProfileInk,2.6f)
        }
    }
}

@Composable
private fun Chip(text: String, bg: Color, fg: Color, icon: String? = null, iconFill:Color? = null) {
    Row(Modifier.background(bg,CircleShape).padding(horizontal=12.dp,vertical=7.dp),
        horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
        if(icon!=null) SimpleIcon(icon,16.dp,if(iconFill==null) fg else YellowEdge,if(iconFill==null) 2.4f else 1.6f,fill=iconFill)
        Label(text,14,FontWeight.ExtraBold,fg)
    }
}

@Composable
private fun StatTile(icon: String, number: String, title: String, modifier: Modifier) {
    Column(modifier.background(Color(0xFFF5F7FB),RoundedCornerShape(20.dp)).padding(horizontal=2.dp,vertical=10.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
        when(icon) {
            "xp" -> SimpleIcon("M13 2 L4 14 L11 14 L10 22 L20 9 L13 9 Z",26.dp,YellowEdge,1.6f,fill=Yellow)
            "streak" -> SimpleIcon("M12 2 C13 6 18 8 18 14 C18 18 15.3 21 12 21 C8.7 21 6 18 6 14.5 C6 11.5 8 10 8.5 7.5 C10 9 10.5 10 10.5 11 C12 9 12.5 5 12 2 Z",26.dp,Color(0xFF8796AB),1.4f,fill=Color(0xFFA9B6C8))
            else -> SimpleIcon("M4 19.5A2.5 2.5 0 0 1 6.5 17H20V3H6.5A2.5 2.5 0 0 0 4 5.5Z M4 19.5A2.5 2.5 0 0 0 6.5 22H20v-5",26.dp,ProfileBlue)
        }
        Label(number,22,FontWeight.Black)
        Label(title,13,FontWeight.Bold,ProfileMuted,maxLines=1)
    }
}

@Composable
private fun ProfileTape(course: Course, progress: ProgressSnapshot, onOpen: (Lesson) -> Unit) {
    val flags = course.lessons.take(4).chunked(2).flatMap { pair ->
        val main = pair.first()
        listOf(main.concept.isEmpty() || main.id in progress.completedBookIds || main.id in progress.completedLessonIds,
            main.id in progress.completedLessonIds,
            pair.getOrNull(1)?.id?.let { it in progress.completedLessonIds } == true)
    }
    val done = flags.takeWhile { it }.size
    val next = course.lessons.firstOrNull { it.id !in progress.completedLessonIds }
    WhiteCard {
        Column(Modifier.fillMaxWidth().padding(start=20.dp,end=20.dp,top=18.dp,bottom=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Label(ProfileStrings.tape,13,FontWeight.ExtraBold,ProfileMuted)
                    Label(ProfileStrings.stageTitle,19,FontWeight.Black,maxLines=1)
                }
                Label("$done/6",15,FontWeight.ExtraBold,ProfileMuted)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal=2.dp,vertical=4.dp),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically) {
                val nodes = listOf("book","1","repeat","book","2","repeat")
                nodes.forEachIndexed { i,node ->
                    val complete = i < done
                    val current = i==done
                    val diameter = if (node=="1" || node=="2") 34.dp else if (node=="book") 32.dp else 26.dp
                    Box(Modifier.size(diameter).solidEdge(diameter/2,4.dp,when { complete -> YellowEdge; current -> Color(0xFF58B3DC); else -> Color(0xFFC8CFD9) })
                        .background(when { complete -> Yellow; current -> Color(0xFF8FD6F5); else -> Color(0xFFEEF1F5) },CircleShape)
                        .then(if (current) Modifier.border(2.dp,Color.White,CircleShape) else Modifier),contentAlignment=Alignment.Center) {
                        if (node=="book") SimpleIcon("M12 6.8C10 5.2 7 4.6 3.5 5v13.2c3.5-.4 6.5.2 8.5 1.8 2-1.6 5-2.2 8.5-1.8V5C17 4.6 14 5.2 12 6.8Z",18.dp,ProfileInk)
                        else if (node=="repeat") SimpleIcon("M4 9a8 8 0 0 1 13-5 M17 4v4h4 M20 15a8 8 0 0 1-13 5 M7 20v-4H3",18.dp,ProfileInk)
                        else Label(node,if (node=="1")15 else 13,FontWeight.Black,if (complete) ProfileInk else Color(0xFF6B7890))
                    }
                }
            }
            ProgressBar(done.toFloat()/6,10.dp)
            next?.let { lesson ->
                val index = course.lessons.indexOf(lesson)
                ProfileAction(if (index % 2 == 0) ProfileStrings.continueLesson(index / 2 + 1) else ProfileStrings.continueReview(index / 2 + 1),50.dp,true) { onOpen(lesson) }
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, height: Dp, track: Color = Color(0xFFE9EDF3)) {
    Box(Modifier.fillMaxWidth().height(height).background(track,CircleShape)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(.04f,1f)).height(height).background(Yellow,CircleShape))
    }
}

@Composable
private fun ProfileCollection(course: Course, progress: ProgressSnapshot, catalog: AvatarCatalog, onAvatar: () -> Unit) {
    val count = catalog.count(progress,course)
    val reward = catalog.nextReward(progress,course)
    val hats = catalog.categories.first { it.id=="hat" }.items
    val closed = hats.filter { !catalog.unlocked(it,progress,course) }
    val shown = listOfNotNull(hats.firstOrNull { it.id==progress.avatar.hat }) +
        (closed.filter { it.unlock.kind in listOf("xp","lessons") } + closed.filter { it.unlock.kind !in listOf("xp","lessons") }).take(3)
    WhiteCard(30.dp) {
        Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Label(ProfileStrings.closet,13,FontWeight.ExtraBold,ProfileMuted); Label(ProfileStrings.collection,19,FontWeight.Black) }
                Chip("${count.first} / ${count.second}",Color(0xFFE8F0FE),ProfileBlue)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                shown.forEach { item ->
                    val locked = !catalog.unlocked(item,progress,course)
                    val preview = progress.avatar.copy(hat=item.id)
                    val colors = catalog.chipStyles[item.unlock.kind]
                    Column(Modifier.weight(1f).height(104.dp).solidEdge(18.dp,4.dp,if (locked) Color(0xFFE6EBF2) else ProfileBlue)
                        .background(if (locked) Color(0xFFF7F9FC) else Color(0xFFEEF4FF),RoundedCornerShape(18.dp))
                        .border(3.dp,if (locked) Color(0xFFEEF1F5) else ProfileBlue,RoundedCornerShape(18.dp))
                        .padding(top=8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(58.dp)) {
                            AvatarView(preview,58.dp,muted=locked)
                            if (locked) Box(Modifier.align(Alignment.TopEnd).size(20.dp).background(ProfileInk,CircleShape),contentAlignment=Alignment.Center) { LockIcon(10.dp) }
                            else Box(Modifier.align(Alignment.TopEnd).size(20.dp).background(ProfileBlue,CircleShape),contentAlignment=Alignment.Center) { Label("✓",13,FontWeight.Black,Color.White) }
                        }
                        if (locked) {
                            Label(item.unlock.chip.replace("lecciones","lecc."),10,FontWeight.Black,
                                colors?.second?.toUiColor() ?: ProfileMuted,
                                Modifier.background(colors?.first?.toUiColor() ?: Color.White,CircleShape).padding(horizontal=4.dp,vertical=2.dp),maxLines=1)
                        } else Label(item.label,11,FontWeight.Black,maxLines=1)
                    }
                }
            }
            reward?.let { (_,item) ->
                val value = catalog.progress(item,progress,course)
                Row(Modifier.fillMaxWidth().background(Color(0xFFFFF8E1),RoundedCornerShape(18.dp))
                    .border(2.dp,Color(0xFFFFE7A3),RoundedCornerShape(18.dp)).padding(12.dp),
                    horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    SimpleIcon("M3 8h18v13H3Z M12 8v13 M3 12h18 M12 8c-2-4-6-4-6-1.5S9 8 12 8c3 0 6-.5 6-1.5S14 4 12 8Z",26.dp,Color(0xFFC98D0B),2.2f,fill=Yellow)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Label("${ProfileStrings.nextReward}: ${item.label}",14,FontWeight.ExtraBold,Color(0xFF6B4D00))
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { ProgressBar(value.first/100f,8.dp,Color(0xFFF3E3B3)) }
                            Label(value.second,12,FontWeight.Black,Color(0xFF6B4D00))
                        }
                    }
                }
            }
            ProfileAction(ProfileStrings.personalize,52.dp,false,onAvatar)
        }
    }
}

private fun String.toUiColor(): Color = Color(android.graphics.Color.parseColor(this))

@Composable
private fun ProfileWeek() {
    val today = LocalDate.now().dayOfWeek.value-1
    WhiteCard {
        Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row { Label(ProfileStrings.week,19,FontWeight.Black,modifier=Modifier.weight(1f)); Label(ProfileStrings.weekCount,14,FontWeight.ExtraBold,ProfileMuted) }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                listOf("L","M","M","J","V","S","D").forEachIndexed { i,day ->
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Label(day,12,FontWeight.ExtraBold,ProfileMuted)
                        Box(Modifier.size(40.dp).background(if(i==today) Color(0xFFFFF5D6) else Color(0xFFF1F4F8),CircleShape)
                            .drawBehind {
                                if (i==today) drawCircle(Color(0xFFF5A623),radius=size.minDimension/2-1.5.dp.toPx(),
                                    style=Stroke(width=3.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),3.dp.toPx()))))
                            },contentAlignment=Alignment.Center) {
                            SimpleIcon("M12 2 C13 6 18 8 18 14 C18 18 15.3 21 12 21 C8.7 21 6 18 6 14.5 C6 11.5 8 10 8.5 7.5 C10 9 10.5 10 10.5 11 C12 9 12.5 5 12 2 Z",18.dp,
                                if(i==today)Color(0xFFF5A623) else Color(0xFFC3CCD8),0f,fill=if(i==today)Color(0xFFF5A623) else Color(0xFFC3CCD8))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().background(Color(0xFFFFF5D6),RoundedCornerShape(16.dp)).padding(horizontal=14.dp,vertical=12.dp),
                horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                SimpleIcon("M12 2 C13 6 18 8 18 14 C18 18 15.3 21 12 21 C8.7 21 6 18 6 14.5 C6 11.5 8 10 8.5 7.5 C10 9 10.5 10 10.5 11 C12 9 12.5 5 12 2 Z",22.dp,Color(0xFFF5A623),0f,fill=Color(0xFFF5A623))
                Label(ProfileStrings.weekHint,14,FontWeight.Bold,Color(0xFF6B4D00))
            }
        }
    }
}

private data class Badge(val label: String,val symbol: String,val unlocked: Boolean)
private val badges = listOf(Badge(ProfileStrings.badgeLabels[0],"1",true),Badge(ProfileStrings.badgeLabels[1],"triangle",true),
    Badge(ProfileStrings.badgeLabels[2],"sen 30°",false),Badge(ProfileStrings.badgeLabels[3],"flame",false),
    Badge(ProfileStrings.badgeLabels[4],"check",false),Badge(ProfileStrings.badgeLabels[5],"cup",false))

@Composable
private fun ProfileAchievements() {
    WhiteCard {
        Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row { Label(ProfileStrings.achievements,19,FontWeight.Black,modifier=Modifier.weight(1f)); Label(ProfileStrings.seeAll,15,FontWeight.ExtraBold,ProfileBlue) }
            badges.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    row.forEach { badge ->
                        Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            val blue=badge.symbol=="triangle"
                            Box(Modifier.size(76.dp).solidEdge(38.dp,6.dp,if(blue)BlueEdge else if (badge.unlocked) YellowEdge else Color(0xFFC8CFD9))
                                .background(if(blue)ProfileBlue else if (badge.unlocked) Yellow else Color(0xFFEEF1F5),CircleShape),contentAlignment=Alignment.Center) {
                                when(badge.symbol) {
                                    "triangle"->SimpleIcon("M3 20 L20 4 L20 20 Z",40.dp,Color.White,0f,fill=Color.White)
                                    "flame"->SimpleIcon("M12 2 C13 6 18 8 18 14 C18 18 15.3 21 12 21 C8.7 21 6 18 6 14.5 C6 11.5 8 10 8.5 7.5 C10 9 10.5 10 10.5 11 C12 9 12.5 5 12 2 Z",32.dp,Color(0xFFC3CCD8),0f,fill=Color(0xFFC3CCD8))
                                    "check"->SimpleIcon("M20 6 9 17l-5-5",32.dp,Color(0xFFA9B6C8),2.6f)
                                    "cup"->SimpleIcon("M8 21h8 M12 17v4 M7 4h10v5a5 5 0 0 1-10 0Z M17 5h3a3 3 0 0 1-3 4 M7 5H4a3 3 0 0 0 3 4",32.dp,Color(0xFFA9B6C8))
                                    else->Label(badge.symbol,if (badge.symbol.length>2)15 else 30,FontWeight.Black,if (badge.unlocked) ProfileInk else Color(0xFF8796AB))
                                }
                            }
                            Label(badge.label,13,FontWeight.ExtraBold,if (badge.unlocked) ProfileInk else ProfileMuted,align=TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

// @spec spec://modules/android/PROP-010-android-demo-architecture#navigation
@Composable
internal fun AvatarEditorScreen(saved: AvatarLook, progress: ProgressSnapshot, course: Course, catalog: AvatarCatalog,
    onBack: () -> Unit, onSave: (AvatarLook) -> Unit) {
    var look by remember(saved) { mutableStateOf(saved) }
    var tab by remember { mutableStateOf("expr") }
    var hint by remember { mutableStateOf<Pair<AvatarCategory,AvatarItem>?>(null) }
    val tabsScroll=rememberScrollState()
    val gridScroll=remember(tab) { ScrollState(0) }
    val scope=rememberCoroutineScope()
    val category = catalog.categories.first { it.id==tab }
    val target = hint ?: catalog.nextReward(progress,course)
    Box(Modifier.fillMaxSize().background(Color(0xFF79C95A))) {
        Meadow(true,Modifier.fillMaxWidth().height(340.dp))
        Row(Modifier.fillMaxWidth().padding(start=16.dp,end=16.dp,top=44.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            EditorTopButton("M6 6l12 12 M18 6L6 18",ProfileInk,ProfileStrings.back,onBack)
            EditorTopButton("M3 8a5 5 0 0 1 5-5h8a5 5 0 0 1 5 5v8a5 5 0 0 1-5 5H8a5 5 0 0 1-5-5Z",ProfileBlue,ProfileStrings.random) {
                look=catalog.randomLook(progress,course);hint=null
            }
        }
        Canvas(Modifier.align(Alignment.TopCenter).offset(y=232.dp).size(270.dp,96.dp)) {
            val scale = size.width/270f
            withTransform({ scale(scale,scale,pivot=Offset.Zero) }) {
                val side=PathParser().parsePathString("M22 30 L22 50 C22 66 72 78 135 78 C198 78 248 66 248 50 L248 30 Z").toPath()
                drawPath(side,Color(0xFFDDE2EA))
                drawOval(Color(0xFFF7F5EF),Offset(22f,6f),Size(226f,48f))
                drawOval(Color.White,Offset(22f,6f),Size(226f,48f),style=Stroke(3f))
            }
        }
        Box(Modifier.align(Alignment.TopCenter).offset(y=70.dp).size(202.dp).background(Color.White,CircleShape).padding(6.dp),contentAlignment=Alignment.Center) {
            AvatarView(look,190.dp)
        }
        Column(Modifier.fillMaxSize().padding(top=318.dp).background(Color.White,RoundedCornerShape(topStart=32.dp,topEnd=32.dp))) {
            Box(Modifier.fillMaxWidth().height(27.dp),contentAlignment=Alignment.Center) {
                Box(Modifier.size(44.dp,5.dp).background(Color(0xFFDFE5EE),CircleShape))
            }
            if (target!=null) RewardBanner(target.first,target.second,look,progress,course,catalog,hint!=null)
            Row(Modifier.fillMaxWidth().horizontalScroll(tabsScroll).padding(start=16.dp,end=16.dp,top=14.dp,bottom=12.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                catalog.categories.forEach { c ->
                    val selected=tab==c.id
                    val real=c.items.filter { it.countsInCollection }
                    val open=real.count { catalog.unlocked(it,progress,course) }
                    Box(Modifier.height(44.dp).solidEdge(22.dp,4.dp,if (selected) BlueEdge else Color(0xFFDFE5EE))
                        .background(if (selected) ProfileBlue else Color(0xFFEEF1F5),CircleShape)
                        .clickable { tab=c.id; hint=null; scope.launch { tabsScroll.scrollTo(0) } }.padding(horizontal=14.dp),contentAlignment=Alignment.Center) {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            SimpleIcon(tabIcon(c.id),20.dp,if (selected) Color.White else ProfileMuted,2.2f)
                            Label(c.tab,16,FontWeight.Black,if (selected) Color.White else ProfileMuted)
                            if (real.isNotEmpty()) Label("$open/${real.size}",12,FontWeight.Black,
                                if (selected) Color.White else ProfileMuted,
                                Modifier.background(if (selected) Color(0x4DFFFFFF) else Color(0xFFDFE5EE),CircleShape).padding(horizontal=6.dp,vertical=2.dp))
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(gridScroll).padding(start=16.dp,end=16.dp,top=4.dp,bottom=116.dp),
                verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment=Alignment.Bottom) {
                    Label(category.title,17,FontWeight.Black,modifier=Modifier.weight(1f))
                    val real=category.items.filter { it.countsInCollection }
                    if (real.isNotEmpty()) Label(ProfileStrings.unlockedCount(real.count { catalog.unlocked(it,progress,course) },real.size),13,FontWeight.ExtraBold,ProfileMuted)
                }
                category.items.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        row.forEach { item ->
                            AvatarItemTile(category,item,look,progress,course,catalog,Modifier.weight(1f)) {
                                if (catalog.unlocked(item,progress,course)) { look=look.with(category.field,item.id);hint=null }
                                else hint=category to item
                            }
                        }
                        repeat(3-row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                category.colors?.let { colors ->
                    Label(colors.title,17,FontWeight.Black,modifier=Modifier.padding(top=6.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        colors.list.forEach { choice ->
                            val selected=look.value(colors.field)==choice.hex
                            Box(Modifier.size(46.dp).solidEdge(23.dp,if (selected)5.dp else 4.dp,if (selected) BlueEdge else ProfileInk.copy(alpha=.14f))
                                .background(choice.hex.toUiColor(),CircleShape)
                                .border(if (selected)4.dp else 3.dp,if (selected)Color.White else ProfileInk.copy(alpha=.08f),CircleShape)
                                .clickable { look=look.with(colors.field,choice.hex);hint=null }
                                .semantics { contentDescription=choice.label; role=Role.Button },contentAlignment=Alignment.Center) {
                                if (selected) SimpleIcon("M20 6 9 17 4 12",18.dp,Color.White,3.6f)
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White)
            .border(1.dp,Color(0xFFEEF1F5)).navigationBarsPadding().padding(start=16.dp,end=16.dp,top=12.dp,bottom=26.dp)) {
            ProfileAction(ProfileStrings.save,56.dp,true) { onSave(look) }
        }
    }
}

private fun tabIcon(id:String)=when(id) {
    "expr"->"M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18 M9 10h.01 M15 10h.01 M8.5 14.5c1.8 1.8 5.2 1.8 7 0"
    "hair"->"M4 15c0-6 3.6-11 8-11s8 5 8 11 M4 15c2.5-3 5-4.5 8-4.5s5.5 1.5 8 4.5 M6 15v5 M18 15v5"
    "hat"->"M3 17h18 M5 17c0-5.5 3-9.5 7-9.5s7 4 7 9.5 M12 7.5V5"
    "glasses"->"M2 12h2 M20 12h2 M10 12h4 M4 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0 M14 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0"
    "outfit"->"M8 3 4 6l2 4 2-1v12h8V9l2 1 2-4-4-3c-1 1.5-2.5 2-4 2s-3-.5-4-2Z"
    else->"M4 4h16v16H4Z M4 15l5-5 5 5 2-2 4 4"
}

@Composable
private fun EditorTopButton(path:String,color:Color,label:String,onClick:()->Unit) {
    Box(Modifier.size(48.dp).solidEdge(16.dp,5.dp,Color(0xFFCFD6E0)).background(Color.White,RoundedCornerShape(16.dp))
        .clickable(onClick=onClick).semantics { contentDescription=label;role=Role.Button },contentAlignment=Alignment.Center) {
        if (label==ProfileStrings.random) {
            Box(Modifier.size(24.dp),contentAlignment=Alignment.Center) {
                SimpleIcon(path,24.dp,color,2.2f)
                Canvas(Modifier.size(24.dp)) {
                    val u=size.width/24f
                    listOf(8f to 8f,16f to 8f,12f to 12f,8f to 16f,16f to 16f).forEach { (x,y) ->
                        drawCircle(color,1.5f*u,Offset(x*u,y*u))
                    }
                }
            }
        } else SimpleIcon(path,24.dp,color,2.8f)
    }
}

@Composable
private fun RewardBanner(category: AvatarCategory, item: AvatarItem, look: AvatarLook, progress: ProgressSnapshot,
    course: Course, catalog: AvatarCatalog, lockedHint:Boolean) {
    val preview=look.with(category.field,item.id)
    val value=catalog.progress(item,progress,course)
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp).background(Color(0xFFFFF8E1),RoundedCornerShape(22.dp))
        .border(2.dp,Color(0xFFFFE7A3),RoundedCornerShape(22.dp)).padding(start=10.dp,end=14.dp,top=10.dp,bottom=10.dp),
        horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(60.dp).background(Color.White,CircleShape).padding(4.dp),contentAlignment=Alignment.Center) {
            AvatarView(preview.copy(bg="#fff1c9"),52.dp)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Label(if(lockedHint) ProfileStrings.locked(item.unlock.chip) else ProfileStrings.nextReward.uppercase(),12,FontWeight.Black,Color(0xFF8A6200),maxLines=1)
            Label(item.label,16,FontWeight.Black,maxLines=1)
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { ProgressBar(value.first/100f,8.dp,Color(0xFFF3E3B3)) }
                Label(value.second,12,FontWeight.Black,Color(0xFF6B4D00),maxLines=1)
            }
        }
    }
}

@Composable
private fun AvatarItemTile(category: AvatarCategory,item: AvatarItem,look: AvatarLook,progress: ProgressSnapshot,
    course: Course,catalog: AvatarCatalog,modifier: Modifier,onClick:()->Unit) {
    val open=catalog.unlocked(item,progress,course)
    val selected=look.value(category.field)==item.id
    val outline=when { selected->ProfileBlue;open->Color(0xFFE6EBF2);else->Color(0xFFEEF1F5) }
    val bg=when { selected->Color(0xFFEEF4FF);open->Color.White;else->Color(0xFFF7F9FC) }
    Box(modifier.height(162.dp).solidEdge(22.dp,5.dp,when { selected->ProfileBlue;open->Color(0xFFDFE5EE);else->Color(0xFFE6EBF2) })
        .background(bg,RoundedCornerShape(22.dp)).border(3.dp,outline,RoundedCornerShape(22.dp))
        .clickable(onClick=onClick).semantics { contentDescription=item.label;role=Role.Button }) {
        Column(Modifier.fillMaxSize().padding(top=12.dp,start=4.dp,end=4.dp,bottom=10.dp),horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(6.dp)) {
            val preview=look.with(category.field,item.id).let { if (category.id=="hat") it else it.copy(hat="none") }
            AvatarView(preview,72.dp,muted=!open)
            Label(item.label,13,FontWeight.Black,align=TextAlign.Center,maxLines=2)
            if(!open) {
                val colors=catalog.chipStyles[item.unlock.kind]
                Label(item.unlock.chip,11,FontWeight.Black,colors?.second?.toUiColor()?:ProfileMuted,
                    Modifier.background(colors?.first?.toUiColor()?:Color.White,CircleShape).padding(horizontal=6.dp,vertical=4.dp),maxLines=1)
            }
        }
        if(selected) Box(Modifier.align(Alignment.TopEnd).offset(x=(-7).dp,y=7.dp).size(26.dp).background(ProfileBlue,CircleShape),contentAlignment=Alignment.Center) {
            Label("✓",18,FontWeight.Black,Color.White)
        }
        if(!open) Box(Modifier.align(Alignment.TopEnd).offset(x=(-7).dp,y=7.dp).size(26.dp).background(ProfileInk,CircleShape),contentAlignment=Alignment.Center) { LockIcon(16.dp) }
    }
}

@Composable
private fun LockIcon(size:Dp) {
    SimpleIcon("M5 11h14v10H5Z M8 11V8a4 4 0 0 1 8 0v3",size,Color.White,3f)
}
