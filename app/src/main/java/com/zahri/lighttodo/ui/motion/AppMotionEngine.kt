package com.zahri.lighttodo.ui.motion

enum class AppMotionEngine {
    PlatformActivityOptions,
    MotionLayout,
    Lottie,
    ComposePrimitive
}

enum class AppMotionUseCase {
    NoteEditorContainerTransform,
    StatefulComponentTransform,
    AssetTimelineAnimation
}

object AppMotionEnginePolicy {
    fun preferredEngines(useCase: AppMotionUseCase): List<AppMotionEngine> =
        when (useCase) {
            AppMotionUseCase.NoteEditorContainerTransform -> listOf(
                AppMotionEngine.PlatformActivityOptions,
                AppMotionEngine.MotionLayout,
                AppMotionEngine.ComposePrimitive
            )
            AppMotionUseCase.StatefulComponentTransform -> listOf(
                AppMotionEngine.MotionLayout,
                AppMotionEngine.ComposePrimitive
            )
            AppMotionUseCase.AssetTimelineAnimation -> listOf(
                AppMotionEngine.Lottie,
                AppMotionEngine.ComposePrimitive
            )
        }
}
