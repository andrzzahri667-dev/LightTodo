package com.zahri.lighttodo

import com.zahri.lighttodo.test.sourcePath
import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureDependencySourceTest {
    @Test
    fun dataLayerDoesNotImportUiLayer() {
        val dataDir = sourcePath("app/src/main/java/com/zahri/lighttodo/data")
        val offenders = dataDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val importsUi = file.readLines().any { line ->
                    line.startsWith("import com.zahri.lighttodo.ui.")
                }
                if (importsUi) file.relativeTo(dataDir).path else null
            }
            .toList()

        assertTrue("data files must not import ui: $offenders", offenders.isEmpty())
    }

    @Test
    fun dataLayerDoesNotImportSystemIntegrationLayers() {
        val dataDir = sourcePath("app/src/main/java/com/zahri/lighttodo/data")
        val offenders = dataDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val importsIntegration = file.readLines().any { line ->
                    line.startsWith("import com.zahri.lighttodo.calendar.") ||
                        line.startsWith("import com.zahri.lighttodo.notify.") ||
                        line.startsWith("import com.zahri.lighttodo.widget.") ||
                        line.startsWith("import com.zahri.lighttodo.integration.")
                }
                if (importsIntegration) file.relativeTo(dataDir).path else null
            }
            .toList()

        assertTrue("data files must not import integration layers: $offenders", offenders.isEmpty())
    }

    @Test
    fun repositoryDoesNotImportSystemIntegrationImplementations() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/todo/TodoRepositoryImpl.kt").readText()

        assertFalse(source.contains("import com.zahri.lighttodo.calendar."))
        assertFalse(source.contains("import com.zahri.lighttodo.notify."))
        assertFalse(source.contains("import com.zahri.lighttodo.widget."))
        assertFalse(source.contains("import com.zahri.lighttodo.integration."))
    }

    @Test
    fun dataLayerDoesNotDefineSystemIntegrationPorts() {
        val dataDir = sourcePath("app/src/main/java/com/zahri/lighttodo/data")
        val offenders = dataDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val source = file.readText()
                val definesGatewayPort =
                    "interface ReminderGateway" in source ||
                        "interface CalendarGateway" in source ||
                        "interface WidgetUpdater" in source
                if (definesGatewayPort) file.relativeTo(dataDir).path else null
            }
            .toList()

        assertTrue("system integration ports belong outside data: $offenders", offenders.isEmpty())
    }

    @Test
    fun todoRepositoryDoesNotCoordinateSystemSideEffects() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/todo/TodoRepositoryImpl.kt").readText()
        val forbiddenTokens = listOf(
            "ReminderGateway",
            "CalendarGateway",
            "WidgetUpdater",
            "reminderGateway",
            "calendarGateway",
            "widgetUpdater",
            "mirrorTodoToCalendar",
            "rescheduleAllAlarms"
        )
        val offenders = forbiddenTokens.filter { it in source }

        assertTrue("Repository must only persist local todo data: $offenders", offenders.isEmpty())
    }

    @Test
    fun todoUseCasesCoordinateSystemSideEffects() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/todo/TodoUseCases.kt").readText()

        assertTrue(source.contains("ReminderGateway"))
        assertTrue(source.contains("CalendarGateway"))
        assertTrue(source.contains("WidgetUpdater"))
        assertTrue(source.contains("calendarGateway.upsertFromTodo"))
        assertTrue(source.contains("calendarGateway.setCompleted"))
        assertTrue(source.contains("calendarGateway.deleteEvent"))
        assertTrue(source.contains("reminderGateway.schedule"))
        assertTrue(source.contains("reminderGateway.cancel"))
        assertTrue(source.contains("widgetUpdater.notifyTodosChanged"))
    }

    @Test
    fun todoBusinessInputAndRulesLiveInDomainLayer() {
        val dataDir = sourcePath("app/src/main/java/com/zahri/lighttodo/data")
        val offenders = dataDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val source = file.readText()
                val declaresTodoBusinessRule =
                    "data class TodoInput" in source ||
                        "object TodoReminderDefaults" in source ||
                        "sealed class TodoDateFields" in source
                if (declaresTodoBusinessRule) file.relativeTo(dataDir).path else null
            }
            .toList()
        val domainDir = sourcePath("app/src/main/java/com/zahri/lighttodo/domain/todo")

        assertTrue("todo business inputs/rules must move out of data: $offenders", offenders.isEmpty())
        assertTrue("domain.todo package must exist", domainDir.exists())
    }

    @Test
    fun noteMarkdownAndAttachmentResponsibilitiesAreSplitByLayer() {
        val rootNoteDir = sourcePath("app/src/main/java/com/zahri/lighttodo/note")
        val rootNoteFiles = if (rootNoteDir.exists()) {
            rootNoteDir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .map { it.name }
                .toList()
        } else {
            emptyList()
        }

        assertTrue("root note package must be split by layer: $rootNoteFiles", rootNoteFiles.isEmpty())
        assertTrue(sourcePath("app/src/main/java/com/zahri/lighttodo/domain/note").exists())
        assertTrue(sourcePath("app/src/main/java/com/zahri/lighttodo/domain/markdown").exists())
        assertTrue(sourcePath("app/src/main/java/com/zahri/lighttodo/data/note").exists())
    }

    @Test
    fun calendarAndReminderPoliciesLiveInPureDomainPackages() {
        val calendarDir = sourcePath("app/src/main/java/com/zahri/lighttodo/calendar")
        val notifyDir = sourcePath("app/src/main/java/com/zahri/lighttodo/notify")
        val misplacedPolicies = (calendarDir.walkTopDown() + notifyDir.walkTopDown())
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val source = file.readText()
                val declaresDomainPolicy =
                    "object CalendarSyncPolicy" in source ||
                        "object CalendarEventWritePolicy" in source ||
                        "object ReminderRequestCodePolicy" in source ||
                        "object ReminderFullScreenPolicy" in source
                if (declaresDomainPolicy) file.relativeTo(sourcePath("app/src/main/java/com/zahri/lighttodo")).path else null
            }
            .toList()

        assertTrue("calendar/notify integration packages must not own domain policies: $misplacedPolicies", misplacedPolicies.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/domain/calendar/CalendarSyncPolicy.kt").readText().contains("object CalendarSyncPolicy"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/domain/calendar/CalendarEventWritePolicy.kt").readText().contains("object CalendarEventWritePolicy"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/domain/reminder/ReminderRequestCodePolicy.kt").readText().contains("object ReminderRequestCodePolicy"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/domain/reminder/ReminderFullScreenPolicy.kt").readText().contains("object ReminderFullScreenPolicy"))
    }

    @Test
    fun domainLayerDoesNotImportAndroidOrDataImplementations() {
        val domainDir = sourcePath("app/src/main/java/com/zahri/lighttodo/domain")
        val offenders = domainDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val importsForbiddenLayer = file.readLines().any { line ->
                    line.startsWith("import android.") ||
                        line.startsWith("import androidx.") ||
                        line.startsWith("import com.zahri.lighttodo.data.")
                }
                if (importsForbiddenLayer) file.relativeTo(domainDir).path else null
            }
            .toList()

        assertTrue("domain must stay pure: $offenders", offenders.isEmpty())
    }

    @Test
    fun todoDisplayTextLivesInDomainAndIntegrationDoesNotImportUi() {
        val integrationDir = sourcePath("app/src/main/java/com/zahri/lighttodo/integration")
        val integrationUiImports = integrationDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val importsUi = file.readLines().any { line ->
                    line.startsWith("import com.zahri.lighttodo.ui.")
                }
                if (importsUi) file.relativeTo(integrationDir).path else null
            }
            .toList()
        val homeViewModelSource = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeViewModel.kt").readText()

        assertTrue("integration files must not import ui helpers: $integrationUiImports", integrationUiImports.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/domain/todo/TodoDisplayText.kt").readText().contains("object TodoDisplayText"))
        assertFalse(homeViewModelSource.contains("fun TodoEntity.displayTitle"))
        assertFalse(homeViewModelSource.contains("fun TodoEntity.dateLabel"))
    }

    @Test
    fun integrationLayerDoesNotImportFeatureLayer() {
        val integrationDir = sourcePath("app/src/main/java/com/zahri/lighttodo/integration")
        val offenders = integrationDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val importsFeature = file.readLines().any { line ->
                    line.startsWith("import com.zahri.lighttodo.feature.")
                }
                if (importsFeature) file.relativeTo(integrationDir).path else null
            }
            .toList()

        assertTrue("integration files must use app-layer launch intents instead of feature activities: $offenders", offenders.isEmpty())
        val launchIntents = sourceFile("app/src/main/java/com/zahri/lighttodo/AppLaunchIntents.kt").readText()
        assertTrue(launchIntents.contains("fun quickAdd"))
        assertTrue(launchIntents.contains("fun reminder"))
    }

    @Test
    fun integrationLayerUsesAppLaunchIntentsInsteadOfMainActivityClass() {
        val integrationDir = sourcePath("app/src/main/java/com/zahri/lighttodo/integration")
        val offenders = integrationDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val source = file.readText()
                if ("import com.zahri.lighttodo.MainActivity" in source) {
                    file.relativeTo(integrationDir).path
                } else {
                    null
                }
            }
            .toList()

        assertTrue("integration files must use AppLaunchIntents for app navigation: $offenders", offenders.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/AppLaunchIntents.kt").readText().contains("fun main"))
    }

    @Test
    fun androidGatewayImplementationsLiveInIntegrationPackages() {
        val mainDir = sourcePath("app/src/main/java/com/zahri/lighttodo")
        val oldIntegrationDirs = listOf(
            sourcePath("app/src/main/java/com/zahri/lighttodo/calendar"),
            sourcePath("app/src/main/java/com/zahri/lighttodo/notify"),
            sourcePath("app/src/main/java/com/zahri/lighttodo/widget")
        )
        val misplacedGateways = oldIntegrationDirs
            .flatMap { dir ->
                dir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .mapNotNull { file ->
                        val source = file.readText()
                        val declaresAndroidGateway =
                            "class AndroidCalendarGateway" in source ||
                                "class AndroidReminderGateway" in source ||
                                "class AndroidWidgetUpdater" in source
                        if (declaresAndroidGateway) file.relativeTo(mainDir).path else null
                    }
                    .toList()
            }

        assertTrue("Android gateway adapters must move to integration packages: $misplacedGateways", misplacedGateways.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/calendar/AndroidCalendarGateway.kt").readText().contains("class AndroidCalendarGateway"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/reminder/AndroidReminderGateway.kt").readText().contains("class AndroidReminderGateway"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/AndroidWidgetUpdater.kt").readText().contains("class AndroidWidgetUpdater"))
    }

    @Test
    fun calendarSystemIntegrationLivesInIntegrationPackage() {
        val calendarDir = sourcePath("app/src/main/java/com/zahri/lighttodo/calendar")
        val rootCalendarFiles = if (calendarDir.exists()) {
            calendarDir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .map { it.name }
                .toList()
        } else {
            emptyList()
        }

        assertTrue("calendar package must move Android integration code under integration/calendar: $rootCalendarFiles", rootCalendarFiles.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/calendar/CalendarSync.kt").readText().contains("object CalendarSync"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/calendar/CalendarObserver.kt").readText().contains("class CalendarObserver"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/calendar/CalendarEventWriter.kt").readText().contains("object CalendarEventWriter"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/calendar/CalendarSyncCoordinator.kt").readText().contains("object CalendarSyncCoordinator"))
    }

    @Test
    fun reminderSystemIntegrationLivesInIntegrationPackages() {
        val notifyDir = sourcePath("app/src/main/java/com/zahri/lighttodo/notify")
        val misplacedSystemFiles = notifyDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val source = file.readText()
                val declaresSystemIntegration =
                    "class BootReceiver" in source ||
                        "class ReminderReceiver" in source ||
                        "object ReminderScheduler" in source ||
                        "object NotificationChannels" in source
                if (declaresSystemIntegration) file.name else null
            }
            .toList()

        assertTrue("reminder/notification system integration must move out of notify: $misplacedSystemFiles", misplacedSystemFiles.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/boot/BootReceiver.kt").readText().contains("class BootReceiver"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/reminder/ReminderReceiver.kt").readText().contains("class ReminderReceiver"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/reminder/ReminderScheduler.kt").readText().contains("object ReminderScheduler"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/notification/NotificationChannels.kt").readText().contains("object NotificationChannels"))
    }

    @Test
    fun widgetSystemIntegrationLivesInIntegrationPackage() {
        val widgetDir = sourcePath("app/src/main/java/com/zahri/lighttodo/widget")
        val rootWidgetFiles = if (widgetDir.exists()) {
            widgetDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .map { it.name }
                .toList()
        } else {
            emptyList()
        }

        assertTrue("widget implementation must move under integration/widget: $rootWidgetFiles", rootWidgetFiles.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/TodoWidgetProvider.kt").readText().contains("class TodoWidgetProvider"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/WidgetActionReceiver.kt").readText().contains("class WidgetActionReceiver"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/TodoWidgetDisplayPolicy.kt").readText().contains("object TodoWidgetDisplayPolicy"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/WidgetAnimation.kt").readText().contains("object WidgetAnimation"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/WidgetUpdateDebouncer.kt").readText().contains("class WidgetUpdateDebouncer"))
    }

    @Test
    fun notifyPackageIsSplitIntoFeatureAndIntegrationLayers() {
        val notifyDir = sourcePath("app/src/main/java/com/zahri/lighttodo/notify")
        val rootNotifyFiles = if (notifyDir.exists()) {
            notifyDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .map { it.name }
                .toList()
        } else {
            emptyList()
        }

        assertTrue("notify package must move UI to feature and system services to integration: $rootNotifyFiles", rootNotifyFiles.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/feature/quickadd/QuickAddActivity.kt").readText().contains("class QuickAddActivity"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/feature/reminder/ReminderActivity.kt").readText().contains("class ReminderActivity"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/integration/notification/QuickAddService.kt").readText().contains("class QuickAddService"))
    }

    @Test
    fun uiFeaturePackagesLiveUnderFeatureLayer() {
        val oldFeatureDirs = listOf(
            sourcePath("app/src/main/java/com/zahri/lighttodo/ui/home"),
            sourcePath("app/src/main/java/com/zahri/lighttodo/ui/edit"),
            sourcePath("app/src/main/java/com/zahri/lighttodo/ui/note"),
            sourcePath("app/src/main/java/com/zahri/lighttodo/ui/settings")
        )
        val misplacedUiFiles = oldFeatureDirs
            .filter { it.exists() }
            .flatMap { dir ->
                dir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .map { it.relativeTo(sourcePath("app/src/main/java/com/zahri/lighttodo/ui")).path }
                    .toList()
            }

        assertTrue("feature UI files must move from ui/* to feature/*: $misplacedUiFiles", misplacedUiFiles.isEmpty())
        assertTrue(sourcePath("app/src/main/java/com/zahri/lighttodo/feature/home").exists())
        assertTrue(sourcePath("app/src/main/java/com/zahri/lighttodo/feature/todoedit").exists())
        assertTrue(sourcePath("app/src/main/java/com/zahri/lighttodo/feature/noteeditor").exists())
        assertTrue(sourcePath("app/src/main/java/com/zahri/lighttodo/feature/settings").exists())
    }

    @Test
    fun backupDtosLiveInDomainLayer() {
        val entitiesSource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/local/Entities.kt").readText()
        val forbiddenDeclarations = listOf(
            "data class BackupBundle",
            "data class BackupTag",
            "data class BackupTodo",
            "data class BackupNote"
        ).filter { it in entitiesSource }

        assertTrue("backup DTOs must move out of Room entities: $forbiddenDeclarations", forbiddenDeclarations.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/domain/backup/BackupDtos.kt").readText().contains("data class BackupBundle"))
    }

    @Test
    fun backupImplementationLivesInDataBackupPackage() {
        val dataDir = sourcePath("app/src/main/java/com/zahri/lighttodo/data")
        val rootBackupFiles = dataDir
            .listFiles()
            .orEmpty()
            .filter {
                it.isFile &&
                    it.extension == "kt" &&
                    (it.name.startsWith("Backup") || it.name == "PortableBackupStore.kt")
            }
            .map { it.name }

        assertTrue("backup implementation files must live under data/backup: $rootBackupFiles", rootBackupFiles.isEmpty())
        assertTrue(sourcePath("app/src/main/java/com/zahri/lighttodo/data/backup").exists())
    }

    @Test
    fun roomAndPreferencesImplementationsLiveInDedicatedDataPackages() {
        val dataDir = sourcePath("app/src/main/java/com/zahri/lighttodo/data")
        val misplacedRootFiles = dataDir
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == "kt" }
            .map { it.name }
            .filter {
                it in setOf(
                    "AppDatabase.kt",
                    "Daos.kt",
                    "Entities.kt",
                    "DatabaseSnapshotExporter.kt",
                    "UserPrefs.kt"
                )
            }

        assertTrue("Room/DataStore implementation files must move out of data root: $misplacedRootFiles", misplacedRootFiles.isEmpty())
        assertTrue(sourcePath("app/src/main/java/com/zahri/lighttodo/data/local").exists())
        assertTrue(sourcePath("app/src/main/java/com/zahri/lighttodo/data/prefs").exists())
    }

    @Test
    fun productionCodeDoesNotUseAppInstanceSingleton() {
        val mainDir = sourcePath("app/src/main/java/com/zahri/lighttodo")
        val offenders = mainDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val source = file.readText()
                if ("App.instance" in source || "lateinit var instance: App" in source) {
                    file.relativeTo(mainDir).path
                } else {
                    null
                }
            }
            .toList()

        assertTrue("production code must not use App.instance: $offenders", offenders.isEmpty())
    }

    @Test
    fun appDoesNotExposeDataDependenciesAsShortcutProperties() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/App.kt").readText()
        val forbiddenTokens = listOf(
            "val db get() =",
            "val prefs get() =",
            "val repository get() =",
            "val backupManager get() ="
        ).filter { it in source }

        assertTrue("App must expose the AppContainer, not individual data dependencies: $forbiddenTokens", forbiddenTokens.isEmpty())
    }

    @Test
    fun featureLayerDoesNotReachIntoAppContainerDataStores() {
        val featureDir = sourcePath("app/src/main/java/com/zahri/lighttodo/feature")
        val forbiddenTokens = listOf(
            ".container.db",
            ".container.prefs",
            ".container.repository",
            ".container.backupManager"
        )
        val offenders = featureDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val source = file.readText()
                val directDataAccess = forbiddenTokens.filter { it in source }
                if (directDataAccess.isNotEmpty()) {
                    "${file.relativeTo(featureDir).path}: $directDataAccess"
                } else {
                    null
                }
            }
            .toList()

        assertTrue("feature files must use injected deps/usecases instead of AppContainer data stores: $offenders", offenders.isEmpty())
    }

    @Test
    fun featureLayerDoesNotReachIntoAppOrContainerDirectly() {
        val featureDir = sourcePath("app/src/main/java/com/zahri/lighttodo/feature")
        val offenders = featureDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val source = file.readText()
                val forbiddenTokens = listOf(
                    "import com.zahri.lighttodo.App",
                    "application as App",
                    ".container."
                ).filter { it in source }
                if (forbiddenTokens.isNotEmpty()) {
                    "${file.relativeTo(featureDir).path}: $forbiddenTokens"
                } else {
                    null
                }
            }
            .toList()

        assertTrue("feature files must use ViewModel injection instead of App/container access: $offenders", offenders.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/feature/quickadd/QuickAddViewModel.kt").readText().contains("class QuickAddViewModel"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/feature/reminder/ReminderViewModel.kt").readText().contains("class ReminderViewModel"))
    }

    @Test
    fun reminderReceiverDoesNotReadTodoDatabaseDirectly() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/reminder/ReminderReceiver.kt").readText()

        assertFalse(source.contains(".container.db"))
        assertFalse(source.contains("TodoEntity"))
    }

    @Test
    fun widgetActionReceiverDoesNotReadTodoDatabaseDirectly() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/WidgetActionReceiver.kt").readText()

        assertFalse(source.contains(".container.db"))
    }

    @Test
    fun todoWidgetProviderDoesNotReadTodoDatabaseDirectly() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/TodoWidgetProvider.kt").readText()

        assertFalse(source.contains(".container.db"))
    }

    @Test
    fun bootReceiverDoesNotReadPreferencesDirectly() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/boot/BootReceiver.kt").readText()

        assertFalse(source.contains(".container.prefs"))
    }

    @Test
    fun calendarSyncEntryPointDoesNotReachIntoAppContainerDataStores() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/calendar/CalendarSync.kt").readText()

        assertFalse(source.contains(".container.db"))
        assertFalse(source.contains(".container.prefs"))
    }

    @Test
    fun noteViewModelsUseNoteUseCasesInsteadOfDaoAndAttachmentStore() {
        val viewModelFiles = listOf(
            sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeViewModel.kt"),
            sourceFile("app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditViewModel.kt")
        )
        val offenders = viewModelFiles
            .mapNotNull { file ->
                val source = file.readText()
                val forbiddenImports = listOf(
                    "import com.zahri.lighttodo.data.local.NoteDao",
                    "import com.zahri.lighttodo.data.note.NoteAttachmentStore"
                ).filter { it in source }
                if (forbiddenImports.isNotEmpty()) {
                    "${file.name}: $forbiddenImports"
                } else {
                    null
                }
            }

        assertTrue("note view models must coordinate through note usecases: $offenders", offenders.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/note/NoteUseCases.kt").readText().contains("class SaveNoteUseCase"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/data/note/NoteRepositoryImpl.kt").readText().contains("class NoteRepositoryImpl"))
    }

    @Test
    fun todoEditViewModelLoadsThroughUseCaseInsteadOfDaosAndPreferences() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/todoedit/EditViewModel.kt").readText()
        val forbiddenImports = listOf(
            "import com.zahri.lighttodo.data.local.TagDao",
            "import com.zahri.lighttodo.data.local.TagEntity",
            "import com.zahri.lighttodo.data.local.TodoDao",
            "import com.zahri.lighttodo.data.prefs.UserPrefs"
        ).filter { it in source }

        assertTrue("EditViewModel must load editor state through usecases: $forbiddenImports", forbiddenImports.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/todo/TodoUseCases.kt").readText().contains("class LoadTodoEditUseCase"))
    }

    @Test
    fun homeViewModelWritesPreferencesThroughUseCases() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeViewModel.kt").readText()

        assertFalse(source.contains("import com.zahri.lighttodo.data.prefs.UserPrefs"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/todo/TodoUseCases.kt").readText().contains("class UpdateHomePreferencesUseCase"))
    }

    @Test
    fun settingsViewModelUsesSettingsUseCasesInsteadOfDataImplementations() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/settings/SettingsViewModel.kt").readText()
        val forbiddenImports = listOf(
            "import com.zahri.lighttodo.data.local.AppDatabase",
            "import com.zahri.lighttodo.data.local.DatabaseSnapshotExporter",
            "import com.zahri.lighttodo.data.prefs.UserPrefs",
            "import com.zahri.lighttodo.data.backup.BackupManager",
            "import com.zahri.lighttodo.integration.calendar.CalendarSync"
        ).filter { it in source }

        assertTrue("SettingsViewModel must use settings usecases instead of data/integration implementations: $forbiddenImports", forbiddenImports.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/settings/SettingsUseCases.kt").readText().contains("class ExportSettingsBackupUseCase"))
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/data/settings/SettingsRepositoryImpl.kt").readText().contains("class SettingsRepositoryImpl"))
    }

    @Test
    fun noteEditorLaunchSeedDoesNotDependOnRoomEntity() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditLaunchSeed.kt").readText()

        assertFalse(source.contains("import com.zahri.lighttodo.data.local.NoteEntity"))
    }

    @Test
    fun homeTodoFeatureUsesUseCaseDtosInsteadOfRoomTodoEntity() {
        val files = listOf(
            sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeViewModel.kt"),
            sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeUiStateMapper.kt"),
            sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeTodoPage.kt"),
            sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeTodoListItems.kt")
        )
        val offenders = files
            .mapNotNull { file ->
                val source = file.readText()
                val forbiddenImports = listOf(
                    "import com.zahri.lighttodo.data.HomeData",
                    "import com.zahri.lighttodo.data.local.TodoEntity"
                ).filter { it in source }
                if (forbiddenImports.isNotEmpty()) {
                    "${file.name}: $forbiddenImports"
                } else {
                    null
                }
            }

        assertTrue("home todo feature must use usecase DTOs instead of Room todo entities: $offenders", offenders.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/todo/TodoUseCases.kt").readText().contains("data class HomeTodoSnapshot"))
    }

    @Test
    fun homeNoteFeatureUsesUseCaseDtosInsteadOfRoomNoteEntity() {
        val files = listOf(
            sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeViewModel.kt"),
            sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/note/NoteGridPage.kt"),
            sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/note/NoteGridItems.kt")
        )
        val offenders = files
            .mapNotNull { file ->
                val source = file.readText()
                if ("import com.zahri.lighttodo.data.local.NoteEntity" in source) {
                    file.name
                } else {
                    null
                }
            }

        assertTrue("home note feature must use usecase DTOs instead of Room note entities: $offenders", offenders.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/note/NoteUseCases.kt").readText().contains("data class NoteListItem"))
    }

    @Test
    fun noteEditorFeatureUsesNoteUseCasesInsteadOfAttachmentStore() {
        val featureNoteDir = sourcePath("app/src/main/java/com/zahri/lighttodo/feature/noteeditor")
        val offenders = featureNoteDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val source = file.readText()
                if ("import com.zahri.lighttodo.data.note.NoteAttachmentStore" in source) {
                    file.relativeTo(featureNoteDir).path
                } else {
                    null
                }
            }
            .toList()

        assertTrue("note editor feature must use note usecases instead of data NoteAttachmentStore: $offenders", offenders.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/note/NoteUseCases.kt").readText().contains("class CopyNoteImageFromUriUseCase"))
    }

    @Test
    fun todoUseCasesDependOnUseCaseInterfacesInsteadOfConcreteDataStores() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/todo/TodoUseCases.kt").readText()
        val forbiddenImports = listOf(
            "import com.zahri.lighttodo.data.HomeData",
            "import com.zahri.lighttodo.data.Repository",
            "import com.zahri.lighttodo.data.prefs.UserPrefs"
        ).filter { it in source }

        assertTrue(
            "todo usecases must depend on usecase-level interfaces, not concrete data stores: $forbiddenImports",
            forbiddenImports.isEmpty()
        )
        assertTrue(source.contains("interface TodoRepository"))
        assertTrue(source.contains("interface HomePreferencesRepository"))
    }

    @Test
    fun calendarAndBootUseCasesDependOnInterfacesInsteadOfConcreteDataStores() {
        val files = listOf(
            sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/calendar/SyncCalendarUseCase.kt"),
            sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/boot/HandleBootCompletedUseCase.kt")
        )
        val forbiddenImports = files
            .flatMap { file ->
                val source = file.readText()
                listOf(
                    "import com.zahri.lighttodo.data.Repository",
                    "import com.zahri.lighttodo.data.prefs.UserPrefs"
                ).filter { it in source }.map { "${file.name}: $it" }
            }

        assertTrue(
            "calendar/boot usecases must depend on usecase-level interfaces, not concrete data stores: $forbiddenImports",
            forbiddenImports.isEmpty()
        )
        assertTrue(files[0].readText().contains("interface CalendarSyncRepository"))
        assertTrue(files[0].readText().contains("interface CalendarSyncPreferencesRepository"))
        assertTrue(files[1].readText().contains("interface BootPreferencesRepository"))
    }

    @Test
    fun useCasesUseTodoRecordInsteadOfRoomTodoEntity() {
        val usecaseDir = sourcePath("app/src/main/java/com/zahri/lighttodo/usecase")
        val offenders = usecaseDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val source = file.readText()
                val forbiddenImports = listOf(
                    "import com.zahri.lighttodo.data.local.TodoEntity",
                    "import com.zahri.lighttodo.data.local.hasAnyReminder"
                ).filter { it in source }
                if (forbiddenImports.isNotEmpty()) {
                    "${file.relativeTo(usecaseDir).path}: $forbiddenImports"
                } else {
                    null
                }
            }
            .toList()

        assertTrue("usecases must use TodoRecord instead of Room TodoEntity: $offenders", offenders.isEmpty())
        assertTrue(sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/todo/TodoUseCases.kt").readText().contains("data class TodoRecord"))
    }

    @Test
    fun featureNotifyAndWidgetLayersDoNotCallRepositoryDirectly() {
        val mainDir = sourcePath("app/src/main/java/com/zahri/lighttodo")
        val scannedDirs = listOf(
            sourcePath("app/src/main/java/com/zahri/lighttodo/ui"),
            sourcePath("app/src/main/java/com/zahri/lighttodo/notify"),
            sourcePath("app/src/main/java/com/zahri/lighttodo/widget")
        )
        val offenders = scannedDirs
            .flatMap { dir ->
                dir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .mapNotNull { file ->
                        val source = file.readText()
                        val importsRepository = "import com.zahri.lighttodo.data.Repository" in source
                        val callsAppRepository = ".repository" in source
                        if (importsRepository || callsAppRepository) {
                            file.relativeTo(mainDir).path
                        } else {
                            null
                        }
                    }
                    .toList()
            }

        assertTrue("feature/notify/widget files must use usecases instead of Repository: $offenders", offenders.isEmpty())
    }
}
