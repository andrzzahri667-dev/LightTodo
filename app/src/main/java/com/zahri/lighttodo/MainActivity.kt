package com.zahri.lighttodo

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zahri.lighttodo.ui.edit.EditScreen
import com.zahri.lighttodo.ui.home.HomeScreen
import com.zahri.lighttodo.ui.motion.AppMotion
import com.zahri.lighttodo.ui.note.NoteEditorLauncher
import com.zahri.lighttodo.ui.note.NoteSourceAnimationKey
import com.zahri.lighttodo.ui.settings.SettingsScreen
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.LightTodoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val sourceAnimationResetHandler = Handler(Looper.getMainLooper())
    private var hiddenNoteSource by mutableStateOf<NoteSourceAnimationKey?>(null)
    private var pendingNoteScaleDownData: NoteEditorLauncher.NoteScaleDownData? = null
    private var noteEditorWasLaunched = false

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
                    val rootView = LocalView.current
                    val density = LocalDensity.current
                    val noteTargetBackground = if (isSystemInDarkTheme()) Color.Black else Color(0xFFFFFCF6)

                    AppNavHost(
                        nav = nav,
                        onNoteEdit = { id, sourceBounds, sourceScale ->
                            val sourceKey = id?.let(NoteSourceAnimationKey::forExistingNote)
                                ?: NoteSourceAnimationKey.forNewNote()
                            val result = NoteEditorLauncher.launch(
                                activity = this@MainActivity,
                                rootView = rootView,
                                noteId = id,
                                sourceBounds = sourceBounds,
                                density = density.density,
                                sourceScale = sourceScale,
                                targetBackgroundColor = noteTargetBackground.toArgb(),
                                createSourceColor = AppColors.Brand.toArgb(),
                                onSourceHiddenChange = { hidden ->
                                    setNoteSourceHidden(sourceKey, hidden)
                                }
                            )
                            replacePendingScaleDownData(result.scaleDownData)
                            noteEditorWasLaunched = true
                        },
                        hiddenNoteSource = hiddenNoteSource
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (noteEditorWasLaunched) {
            pendingNoteScaleDownData?.let {
                NoteEditorLauncher.updateScaleDownData(this, it)
            }
            noteEditorWasLaunched = false
        }
    }

    override fun onDestroy() {
        sourceAnimationResetHandler.removeCallbacksAndMessages(null)
        replacePendingScaleDownData(null)
        super.onDestroy()
    }

    private fun replacePendingScaleDownData(data: NoteEditorLauncher.NoteScaleDownData?) {
        if (pendingNoteScaleDownData !== data) {
            pendingNoteScaleDownData?.recycle()
        }
        pendingNoteScaleDownData = data
    }

    private fun setNoteSourceHidden(sourceKey: NoteSourceAnimationKey, hidden: Boolean) {
        if (hidden) {
            hiddenNoteSource = sourceKey
            sourceAnimationResetHandler.postDelayed(
                {
                    if (hiddenNoteSource == sourceKey) {
                        hiddenNoteSource = null
                    }
                },
                SourceAnimationResetDelayMillis
            )
        } else if (hiddenNoteSource == sourceKey) {
            hiddenNoteSource = null
        }
    }

    private companion object {
        const val SourceAnimationResetDelayMillis = 1_200L
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
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?id=$id"
}

@androidx.compose.runtime.Composable
private fun AppNavHost(
    nav: NavHostController,
    onNoteEdit: (Long?, Rect?, Float) -> Unit,
    hiddenNoteSource: NoteSourceAnimationKey?
) {
    NavHost(
        navController = nav,
        startDestination = Routes.Home,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                AppMotion.routeTween()
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                AppMotion.routeTween()
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                AppMotion.routeTween()
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                AppMotion.routeTween()
            )
        }
    ) {
        composable(Routes.Home) {
            HomeScreen(
                onAdd = { nav.navigate(Routes.edit()) },
                onEdit = { id -> nav.navigate(Routes.edit(id)) },
                onNoteEdit = onNoteEdit,
                hiddenNoteSource = hiddenNoteSource,
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
    }
}
