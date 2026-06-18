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
import com.zahri.lighttodo.feature.todoedit.EditScreen
import com.zahri.lighttodo.feature.home.HomeScreen
import com.zahri.lighttodo.ui.motion.components.NoteSourceVisibilityMotion
import com.zahri.lighttodo.ui.motion.components.motionRouteEnterTransition
import com.zahri.lighttodo.ui.motion.components.motionRouteExitTransition
import com.zahri.lighttodo.ui.motion.components.motionRoutePopEnterTransition
import com.zahri.lighttodo.ui.motion.components.motionRoutePopExitTransition
import com.zahri.lighttodo.feature.noteeditor.NoteEditLaunchSeed
import com.zahri.lighttodo.feature.noteeditor.NoteEditorColors
import com.zahri.lighttodo.feature.noteeditor.NoteEditorLauncher
import com.zahri.lighttodo.feature.noteeditor.NoteScaleDownUpdateAction
import com.zahri.lighttodo.feature.noteeditor.NoteScaleDownUpdatePolicy
import com.zahri.lighttodo.feature.noteeditor.NoteSourceAnimationKey
import com.zahri.lighttodo.feature.settings.SettingsScreen
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.LightTodoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val sourceAnimationResetHandler = Handler(Looper.getMainLooper())
    private var hiddenNoteSource by mutableStateOf<NoteSourceAnimationKey?>(null)
    private var noteEditorWasLaunched = false
    private var noteEditorMiuiReturnAnimationPrepared = false

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val app = application as App
            val storageGranted = results.entries.any {
                (it.key == android.Manifest.permission.READ_EXTERNAL_STORAGE ||
                    it.key == android.Manifest.permission.WRITE_EXTERNAL_STORAGE) && it.value
            }
            if (storageGranted) app.retryRestore()
            if (PermissionRequestPolicy.calendarPermissionsGranted(results)) {
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

        setContent {
            LightTodoTheme {
                Surface(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                    val nav: NavHostController = rememberNavController()
                    val rootView = LocalView.current
                    val density = LocalDensity.current
                    val noteTargetBackground = NoteEditorColors.editorBackground(isSystemInDarkTheme())

                    AppNavHost(
                        nav = nav,
                        onNoteEdit = { id, sourceBounds, sourceScale, launchSeed ->
                            val sourceKey = id?.let(NoteSourceAnimationKey::forExistingNote)
                                ?: NoteSourceAnimationKey.forNewNote()
                            val result = NoteEditorLauncher.launch(
                                activity = this@MainActivity,
                                rootView = rootView,
                                noteId = id,
                                launchSeed = launchSeed,
                                sourceBounds = sourceBounds,
                                density = density.density,
                                sourceScale = sourceScale,
                                targetBackgroundColor = noteTargetBackground.toArgb(),
                                createSourceColor = AppColors.Brand.toArgb(),
                                onSourceHiddenChange = { hidden ->
                                    setNoteSourceHidden(sourceKey, hidden)
                                }
                            )
                            noteEditorWasLaunched = true
                            noteEditorMiuiReturnAnimationPrepared = result.miuiReturnAnimationPrepared
                        },
                        hiddenNoteSource = hiddenNoteSource,
                        onRequestExactAlarmPermission = ::requestExactAlarmPermissionForReminder
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        when (
            NoteScaleDownUpdatePolicy.actionFor(
                editorWasLaunched = noteEditorWasLaunched,
                miuiReturnAnimationPrepared = noteEditorMiuiReturnAnimationPrepared
            )
        ) {
            NoteScaleDownUpdateAction.None -> Unit
            NoteScaleDownUpdateAction.KeepSystemReturn -> Unit
            NoteScaleDownUpdateAction.DisableAnimation -> {
                NoteEditorLauncher.disableScaleDownAnimation(this)
            }
        }
        if (noteEditorWasLaunched) {
            noteEditorWasLaunched = false
            noteEditorMiuiReturnAnimationPrepared = false
        }
    }

    override fun onDestroy() {
        sourceAnimationResetHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
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
                NoteSourceVisibilityMotion.ResetDelayMillis
            )
        } else if (hiddenNoteSource == sourceKey) {
            hiddenNoteSource = null
        }
    }

    private fun hasCalendarPermission(): Boolean =
        PermissionRequestPolicy.calendarPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

    private fun enableCalendarSync() {
        val app = application as App
        app.appScope.launch { app.prefs.setCalendarSyncEnabled(true) }
    }

    private fun requestExactAlarmPermissionForReminder(hasReminder: Boolean) {
        val am = getSystemService(android.app.AlarmManager::class.java)
        val canScheduleExactAlarms =
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
                am?.canScheduleExactAlarms() == true
        if (
            ExactAlarmPermissionPolicy.shouldRequestSettings(
                canScheduleExactAlarms = canScheduleExactAlarms,
                hasReminder = hasReminder
            )
        ) {
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:$packageName"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
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
    onNoteEdit: (Long?, Rect?, Float, NoteEditLaunchSeed?) -> Unit,
    hiddenNoteSource: NoteSourceAnimationKey?,
    onRequestExactAlarmPermission: (Boolean) -> Unit
) {
    NavHost(
        navController = nav,
        startDestination = Routes.Home,
        enterTransition = { motionRouteEnterTransition() },
        exitTransition = { motionRouteExitTransition() },
        popEnterTransition = { motionRoutePopEnterTransition() },
        popExitTransition = { motionRoutePopExitTransition() }
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
            EditScreen(
                editingId = id,
                onBack = { nav.popBackStack() },
                onRequestExactAlarmPermission = onRequestExactAlarmPermission
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
