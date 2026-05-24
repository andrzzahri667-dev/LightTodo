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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zahri.lighttodo.ui.edit.EditScreen
import com.zahri.lighttodo.ui.home.HomeScreen
import com.zahri.lighttodo.ui.motion.AppMotion
import com.zahri.lighttodo.ui.note.NoteContainerTransformDirection
import com.zahri.lighttodo.ui.note.NoteContainerTransformPolicy
import com.zahri.lighttodo.ui.note.NoteEditScreen
import com.zahri.lighttodo.ui.note.NoteRouteBackgroundBehavior
import com.zahri.lighttodo.ui.note.NoteRouteTransitionPolicy
import com.zahri.lighttodo.ui.settings.SettingsScreen
import com.zahri.lighttodo.ui.theme.LightTodoTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
                    val noteTransformRequest = remember { mutableStateOf<NoteContainerTransformRequest?>(null) }
                    val noteTransformKey = remember { mutableStateOf(0L) }

                    fun startNoteContainerTransform(
                        direction: NoteContainerTransformDirection,
                        sourceBounds: Rect?,
                        afterTransform: () -> Unit = {}
                    ) {
                        val rootSize = navRootSize.value
                        if (sourceBounds == null || rootSize.width <= 0 || rootSize.height <= 0) {
                            afterTransform()
                            return
                        }
                        noteTransformKey.value += 1L
                        noteTransformRequest.value = NoteContainerTransformRequest(
                            key = noteTransformKey.value,
                            sourceBounds = sourceBounds,
                            rootSize = rootSize,
                            direction = direction,
                            onFinished = {
                                afterTransform()
                                noteTransformRequest.value = null
                            }
                        )
                    }

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
                                startNoteContainerTransform(NoteContainerTransformDirection.Enter, sourceBounds)
                                nav.navigate(Routes.noteEdit(id))
                            },
                            onNoteBack = { nav.popBackStack() }
                        )
                        NoteContainerTransformOverlay(noteTransformRequest.value)
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

private data class NoteContainerTransformRequest(
    val key: Long,
    val sourceBounds: Rect,
    val rootSize: IntSize,
    val direction: NoteContainerTransformDirection,
    val onFinished: () -> Unit
)

@Composable
private fun NoteContainerTransformOverlay(request: NoteContainerTransformRequest?) {
    if (request == null) return

    val density = LocalDensity.current
    val progress = remember(request.key) {
        Animatable(
            if (request.direction == NoteContainerTransformDirection.Enter) 0f else 1f
        )
    }

    LaunchedEffect(request.key) {
        val target = if (request.direction == NoteContainerTransformDirection.Enter) 1f else 0f
        val animationSpec = if (request.direction == NoteContainerTransformDirection.Enter) {
            AppMotion.noteEnterTween<Float>()
        } else {
            AppMotion.noteExitTween<Float>()
        }
        progress.animateTo(target, animationSpec)
        request.onFinished()
    }

    val bounds = NoteContainerTransformPolicy.boundsAt(
        progress = progress.value,
        sourceBounds = request.sourceBounds,
        rootSize = request.rootSize
    )
    val alpha = NoteContainerTransformPolicy.overlayAlphaAt(request.direction, progress.value)
    val cornerRadius = NoteContainerTransformPolicy.cornerRadiusAt(progress.value).dp
    val background = if (isSystemInDarkTheme()) Color.Black else Color(0xFFFFFCF6)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(4f)
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = bounds.left.roundToInt(),
                        y = bounds.top.roundToInt()
                    )
                }
                .size(
                    width = with(density) { bounds.width.toDp() },
                    height = with(density) { bounds.height.toDp() }
                )
                .alpha(alpha)
                .clip(RoundedCornerShape(cornerRadius))
                .background(background)
        )
    }
}

@androidx.compose.runtime.Composable
private fun AppNavHost(
    nav: NavHostController,
    noteTransitionSourceBounds: () -> Rect?,
    noteTransitionRootSize: () -> IntSize,
    onNoteEdit: (Long?, Rect?) -> Unit,
    onNoteBack: () -> Unit
) {
    NavHost(
        navController = nav,
        startDestination = Routes.Home,
        enterTransition = {
            if (targetState.destination.route == Routes.NoteEditWithId) {
                val spec = NoteRouteTransitionPolicy.specFor(
                    sourceBounds = noteTransitionSourceBounds(),
                    rootSize = noteTransitionRootSize()
                )
                if (spec.hasSourceBounds) {
                    EnterTransition.None
                } else {
                    fadeIn(tween(AppMotion.NoteEnterFadeMillis, easing = AppMotion.EmphasizedEasing)) +
                        scaleIn(
                            animationSpec = AppMotion.noteEnterTween(),
                            initialScale = spec.sourceScale,
                            transformOrigin = spec.transformOrigin
                        )
                }
            } else {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    AppMotion.routeTween()
                )
            }
        },
        exitTransition = {
            when (NoteRouteTransitionPolicy.exitBehaviorForTarget(targetState.destination.route, Routes.NoteEditWithId)) {
                NoteRouteBackgroundBehavior.KeepVisible -> ExitTransition.None
                NoteRouteBackgroundBehavior.StandardSlide -> slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    AppMotion.routeTween()
                )
            }
        },
        popEnterTransition = {
            when (NoteRouteTransitionPolicy.popEnterBehaviorForInitial(initialState.destination.route, Routes.NoteEditWithId)) {
                NoteRouteBackgroundBehavior.KeepVisible -> EnterTransition.None
                NoteRouteBackgroundBehavior.StandardSlide -> slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    AppMotion.routeTween()
                )
            }
        },
        popExitTransition = {
            if (initialState.destination.route == Routes.NoteEditWithId) {
                val spec = NoteRouteTransitionPolicy.specFor(
                    sourceBounds = noteTransitionSourceBounds(),
                    rootSize = noteTransitionRootSize()
                )
                if (spec.hasSourceBounds) {
                    slideOut(
                        animationSpec = AppMotion.noteExitTween(),
                        targetOffset = { spec.sourceCenterOffset }
                    ) +
                        shrinkOut(
                            animationSpec = AppMotion.noteExitTween(),
                            shrinkTowards = Alignment.Center,
                            targetSize = { spec.sourceSize }
                        )
                } else {
                    fadeOut(tween(AppMotion.NoteExitFadeMillis, easing = AppMotion.EmphasizedEasing)) +
                        scaleOut(
                            animationSpec = AppMotion.noteExitTween(),
                            targetScale = spec.sourceScale,
                            transformOrigin = spec.transformOrigin
                        )
                }
            } else {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    AppMotion.routeTween()
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
            NoteEditScreen(editingId = id, onBack = onNoteBack)
        }
    }
}
