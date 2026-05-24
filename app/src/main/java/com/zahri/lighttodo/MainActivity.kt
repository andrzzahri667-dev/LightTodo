package com.zahri.lighttodo

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zahri.lighttodo.ui.edit.EditScreen
import com.zahri.lighttodo.ui.home.HomeScreen
import com.zahri.lighttodo.ui.note.NoteEditScreen
import com.zahri.lighttodo.ui.note.NoteRouteTransitionPolicy
import com.zahri.lighttodo.ui.settings.SettingsScreen
import com.zahri.lighttodo.ui.theme.LightTodoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val app = application as App
            val storageGranted = results.entries.any {
                (it.key == android.Manifest.permission.READ_EXTERNAL_STORAGE ||
                    it.key == android.Manifest.permission.WRITE_EXTERNAL_STORAGE) && it.value
            }
            if (storageGranted) app.retryRestore()
            if (results[android.Manifest.permission.READ_CALENDAR] == true) {
                enableCalendarSync()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startupPermissions = PermissionRequestPolicy.startupPermissions()
        if (startupPermissions.isNotEmpty()) {
            permLauncher.launch(startupPermissions.toTypedArray())
        }
        if (hasCalendarPermission()) {
            enableCalendarSync()
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            (application as App).retryRestore()
        }

        // Hint user to enable exact alarms on Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val am = getSystemService(android.app.AlarmManager::class.java)
            if (am != null && !am.canScheduleExactAlarms()) {
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:$packageName"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }

        setContent {
            LightTodoTheme {
                Surface(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                    val nav: NavHostController = rememberNavController()
                    val noteTransitionSourceBounds = remember { mutableStateOf<Rect?>(null) }
                    val navRootSize = remember { mutableStateOf(IntSize.Zero) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { navRootSize.value = it }
                    ) {
                        AppNavHost(
                            nav = nav,
                            noteTransitionSourceBounds = { noteTransitionSourceBounds.value },
                            noteTransitionRootSize = { navRootSize.value },
                            onNoteEdit = { id, sourceBounds ->
                                noteTransitionSourceBounds.value = sourceBounds
                                nav.navigate(Routes.noteEdit(id))
                            }
                        )
                    }
                }
            }
        }
    }

    private fun hasCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    private fun enableCalendarSync() {
        val app = application as App
        app.appScope.launch { app.prefs.setCalendarSyncEnabled(true) }
    }
}

object Routes {
    const val Home = "home"
    const val Edit = "edit"
    const val EditWithId = "edit?id={id}"
    const val Settings = "settings"
    const val NoteEdit = "note_edit"
    const val NoteEditWithId = "note_edit?id={id}"
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?id=$id"
    fun noteEdit(id: Long? = null) = if (id == null) "note_edit" else "note_edit?id=$id"
}

@androidx.compose.runtime.Composable
private fun AppNavHost(
    nav: NavHostController,
    noteTransitionSourceBounds: () -> Rect?,
    noteTransitionRootSize: () -> IntSize,
    onNoteEdit: (Long?, Rect?) -> Unit
) {
    // Gentle non-linear curve: slow ease-out with longer duration
    val iosEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    val noteEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val duration = 400
    NavHost(
        navController = nav,
        startDestination = Routes.Home,
        enterTransition = {
            if (targetState.destination.route == Routes.NoteEditWithId) {
                val spec = NoteRouteTransitionPolicy.specFor(
                    sourceBounds = noteTransitionSourceBounds(),
                    rootSize = noteTransitionRootSize()
                )
                fadeIn(tween(140, easing = noteEasing)) +
                    slideIn(
                        animationSpec = tween(360, easing = noteEasing),
                        initialOffset = { spec.sourceCenterOffset }
                    ) +
                    scaleIn(
                        animationSpec = tween(360, easing = noteEasing),
                        initialScale = spec.sourceScale,
                        transformOrigin = spec.transformOrigin
                    )
            } else {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(duration, easing = iosEasing)
                )
            }
        },
        exitTransition = {
            if (targetState.destination.route == Routes.NoteEditWithId) {
                fadeOut(tween(90, easing = noteEasing))
            } else {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(duration, easing = iosEasing)
                )
            }
        },
        popEnterTransition = {
            if (initialState.destination.route == Routes.NoteEditWithId) {
                fadeIn(tween(90, easing = noteEasing))
            } else {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(duration, easing = iosEasing)
                )
            }
        },
        popExitTransition = {
            if (initialState.destination.route == Routes.NoteEditWithId) {
                val spec = NoteRouteTransitionPolicy.specFor(
                    sourceBounds = noteTransitionSourceBounds(),
                    rootSize = noteTransitionRootSize()
                )
                fadeOut(tween(120, easing = noteEasing)) +
                    slideOut(
                        animationSpec = tween(300, easing = noteEasing),
                        targetOffset = { spec.sourceCenterOffset }
                    ) +
                    scaleOut(
                        animationSpec = tween(300, easing = noteEasing),
                        targetScale = spec.sourceScale,
                        transformOrigin = spec.transformOrigin
                    )
            } else {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(duration, easing = iosEasing)
                )
            }
        }
    ) {
        composable(Routes.Home) {
            HomeScreen(
                onAdd = { nav.navigate(Routes.edit()) },
                onEdit = { id -> nav.navigate(Routes.edit(id)) },
                onNoteEdit = onNoteEdit,
                onSettings = { nav.navigate(Routes.Settings) }
            )
        }
        composable(
            Routes.EditWithId,
            arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull()
            EditScreen(editingId = id, onBack = { nav.popBackStack() })
        }
        composable(Routes.Settings) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(
            Routes.NoteEditWithId,
            arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull()
            NoteEditScreen(editingId = id, onBack = { nav.popBackStack() })
        }
    }
}
