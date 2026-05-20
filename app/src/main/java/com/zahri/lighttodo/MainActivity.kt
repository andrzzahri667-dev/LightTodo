package com.zahri.lighttodo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zahri.lighttodo.ui.edit.EditScreen
import com.zahri.lighttodo.ui.home.HomeScreen
import com.zahri.lighttodo.ui.note.NoteEditScreen
import com.zahri.lighttodo.ui.settings.SettingsScreen
import com.zahri.lighttodo.ui.theme.LightTodoTheme

class MainActivity : ComponentActivity() {

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val storageGranted = results.entries.any { it.key != android.Manifest.permission.POST_NOTIFICATIONS && it.value }
            if (storageGranted) (application as App).retryRestore()
        }

    private val manageStorageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && android.os.Environment.isExternalStorageManager()) {
                (application as App).retryRestore()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: need MANAGE_EXTERNAL_STORAGE for post-uninstall restore
            val perms = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms += android.Manifest.permission.POST_NOTIFICATIONS
            }
            if (perms.isNotEmpty()) permLauncher.launch(perms.toTypedArray())
            // Request all-files access if not granted (needed to read backup after reinstall)
            if (!android.os.Environment.isExternalStorageManager()) {
                runCatching {
                    manageStorageLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            .setData(Uri.parse("package:$packageName"))
                    )
                }
            } else {
                (application as App).retryRestore()
            }
        } else {
            // Android 10 and below
            val perms = mutableListOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            permLauncher.launch(perms.toTypedArray())
        }

        // Hint user to enable exact alarms on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                    AppNavHost(nav)
                }
            }
        }
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
private fun AppNavHost(nav: NavHostController) {
    // Gentle non-linear curve: slow ease-out with longer duration
    val iosEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    val noteEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val duration = 400
    NavHost(
        navController = nav,
        startDestination = Routes.Home,
        enterTransition = {
            if (targetState.destination.route == Routes.NoteEditWithId) {
                fadeIn(tween(180, easing = noteEasing)) +
                    slideInVertically(tween(320, easing = noteEasing)) { it / 12 } +
                    scaleIn(tween(320, easing = noteEasing), initialScale = 0.985f)
            } else {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(duration, easing = iosEasing)
                )
            }
        },
        exitTransition = {
            if (targetState.destination.route == Routes.NoteEditWithId) {
                fadeOut(tween(120, easing = noteEasing)) +
                    scaleOut(tween(160, easing = noteEasing), targetScale = 0.995f)
            } else {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(duration, easing = iosEasing)
                )
            }
        },
        popEnterTransition = {
            if (initialState.destination.route == Routes.NoteEditWithId) {
                fadeIn(tween(160, easing = noteEasing)) +
                    scaleIn(tween(220, easing = noteEasing), initialScale = 0.995f)
            } else {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(duration, easing = iosEasing)
                )
            }
        },
        popExitTransition = {
            if (initialState.destination.route == Routes.NoteEditWithId) {
                fadeOut(tween(150, easing = noteEasing)) +
                    slideOutVertically(tween(240, easing = noteEasing)) { it / 10 } +
                    scaleOut(tween(220, easing = noteEasing), targetScale = 0.985f)
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
                onNoteEdit = { id -> nav.navigate(Routes.noteEdit(id)) },
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
