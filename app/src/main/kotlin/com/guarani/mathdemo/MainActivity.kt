package com.guarani.mathdemo

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.guarani.mathdemo.progress.ProgressRepository
import com.guarani.mathdemo.ui.CourseApp
import kotlinx.coroutines.runBlocking

// @spec spec://modules/android/PROP-010-android-demo-architecture#navigation
// @spec spec://modules/learning/FEAT-010-learning-demo#language-and-visuals
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightSystemBars()
        // Preview mode (adb only): `-e preview courses/trigonometry.json` or `-e preview templates/exercise-templates.json`
        // opens every lesson with a throwaway progress store, so screens can be reviewed without touching real progress.
        val preview = intent.getStringExtra("preview")?.takeIf { it.startsWith("courses/") || it.startsWith("templates/") }
        val repository = ProgressRepository(applicationContext, preview = preview != null)
        if (preview != null && savedInstanceState == null) runBlocking { repository.clear() }
        setContent {
            val colors = remember {
                androidx.compose.material3.lightColorScheme(
                    primary = Color(0xFF2865C7),
                    onPrimary = Color.White,
                    secondary = Color(0xFF4AB6EA),
                    background = Color(0xFFF2F7FF),
                    surface = Color.White,
                    onSurface = Color(0xFF1E2B43),
                    onSurfaceVariant = Color(0xFF66728A),
                )
            }
            MaterialTheme(colorScheme = colors) {
                if (preview != null) CourseApp(
                    repository, preview, unlockAll = true,
                    startExerciseId = intent.getStringExtra("exercise"),
                    previewReachedLessonId = intent.getStringExtra("reached"),
                ) else CourseApp(repository)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setLightSystemBars() {
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }
}
