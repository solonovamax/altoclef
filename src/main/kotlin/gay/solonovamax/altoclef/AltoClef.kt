package gay.solonovamax.altoclef

import adris.altoclef.AltoClefCommands
import adris.altoclef.BotBehaviour
import adris.altoclef.Debug
import adris.altoclef.Settings
import adris.altoclef.butler.Butler
import adris.altoclef.chains.DeathMenuChain
import adris.altoclef.chains.FoodChain
import adris.altoclef.chains.MLGBucketFallChain
import adris.altoclef.chains.MobDefenseChain
import adris.altoclef.chains.PlayerInteractionFixChain
import adris.altoclef.chains.UserTaskChain
import adris.altoclef.chains.WorldSurvivalChain
import adris.altoclef.commandsystem.CommandExecutor
import adris.altoclef.control.InputControls
import adris.altoclef.control.PlayerExtraController
import adris.altoclef.control.SlotHandler
import adris.altoclef.tasksystem.Task
import adris.altoclef.tasksystem.TaskRunner
import adris.altoclef.trackers.BlockTracker
import adris.altoclef.trackers.EntityTracker
import adris.altoclef.trackers.MiscBlockTracker
import adris.altoclef.trackers.SimpleChunkTracker
import adris.altoclef.trackers.TrackerManager
import adris.altoclef.trackers.storage.ContainerSubTracker
import adris.altoclef.trackers.storage.ItemStorageTracker
import adris.altoclef.ui.CommandStatusOverlay
import adris.altoclef.ui.MessagePriority
import adris.altoclef.ui.MessageSender
import adris.altoclef.util.helpers.InputHelper
import baritone.Baritone
import baritone.altoclef.AltoClefSettings
import baritone.api.BaritoneAPI
import ca.solostudios.adventure.kotlin.dsl.text
import cloud.commandframework.fabric.FabricClientCommandManager
import cloud.commandframework.fabric.argument.FabricArgumentParsers
import cloud.commandframework.kotlin.coroutines.annotations.installCoroutineSupport
import cloud.commandframework.meta.SimpleCommandMeta
import cloud.commandframework.minecraft.extras.AudienceProvider
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler
import cloud.commandframework.minecraft.extras.MinecraftHelp
import gay.solonovamax.altoclef.command.CoreCommands
import gay.solonovamax.altoclef.command.InventoryCommands
import gay.solonovamax.altoclef.command.MiscCommands
import gay.solonovamax.altoclef.command.ResourceCommands
import gay.solonovamax.altoclef.command.SpeedrunCommands
import gay.solonovamax.altoclef.command.UtilCommands
import gay.solonovamax.altoclef.command.source.ClientCommand
import gay.solonovamax.altoclef.util.AnnotationParser
import gay.solonovamax.altoclef.util.ScheduledThreadPool
import gay.solonovamax.altoclef.util.currentThread
import gay.solonovamax.altoclef.util.parseCommands
import gay.solonovamax.altoclef.util.registerParserSupplier
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.world.ClientWorld
import net.minecraft.command.argument.ItemPredicateArgumentType
import net.minecraft.command.argument.ItemPredicateArgumentType.ItemStackPredicateArgument
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import org.lwjgl.glfw.GLFW
import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.ThreadFactory
import java.util.function.Consumer
import baritone.api.Settings as BaritoneSettings
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator as AsyncCommandCoordinator

/**
 * Central access point for AltoClef
 */
object AltoClef : ModInitializer {
    val scheduledThreadPool = ScheduledThreadPool(Runtime.getRuntime().availableProcessors(), AltoClefThreadFactory)
    val coroutineDispatcher: ExecutorCoroutineDispatcher = scheduledThreadPool.asCoroutineDispatcher()
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + coroutineDispatcher)

    val messagePrefix = text {
        color(NamedTextColor.AQUA)
        content("[ALTOCLEF] ")
    }


    // Static access to altoclef
    private val postInitQueue: Queue<Consumer<AltoClef>> = ArrayDeque()

    /**
     * Executes commands (ex. `@get`/`@gamer`)
     */
    val commandExecutor: CommandExecutor = CommandExecutor()

    // Are we in game (playing in a server/world)
    @JvmStatic
    fun inGame(): Boolean {
        // can this be replaced with `MinecraftClient.getInstance().world == null`?
        return MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().networkHandler != null
    }

    /**
     * Runs the highest priority task chain
     * (task chains run the task tree)
     */
    val taskRunner: TaskRunner = TaskRunner()

    private val trackerManager: TrackerManager = TrackerManager()

    /**
     * Controls bot behaviours, like whether to temporarily "protect" certain blocks or items
     */
    val behaviour: BotBehaviour = BotBehaviour()

    /**
     * Extra controls not present in ClientPlayerInteractionManager. This REALLY should be made static or combined with something else.
     */
    val controllerExtras: PlayerExtraController = PlayerExtraController()

    /**
     * The user task chain (runs your command. Ex. Get Diamonds, Beat the Game)
     */
    // Task chains
    val userTaskChain: UserTaskChain = UserTaskChain(taskRunner)

    /**
     * Takes control away to eat food
     */
    val foodChain: FoodChain = FoodChain(taskRunner)

    /**
     * Takes control away to defend against mobs
     */
    val mobDefenseChain: MobDefenseChain = MobDefenseChain(taskRunner)

    /**
     * Takes control away to perform bucket saves
     */
    val mLGBucketChain: MLGBucketFallChain = MLGBucketFallChain(taskRunner)

    private val containerSubTracker: ContainerSubTracker = ContainerSubTracker(trackerManager)

    /**
     * Tracks items in your inventory and in storage containers.
     */
    // Trackers
    val itemStorage: ItemStorageTracker = ItemStorageTracker(trackerManager, containerSubTracker)

    /**
     * Tracks loaded entities
     */
    val entityTracker: EntityTracker = EntityTracker(trackerManager)

    /**
     * Tracks blocks and their positions
     */
    val blockTracker: BlockTracker = BlockTracker(trackerManager)

    /**
     * Tracks of whether a chunk is loaded/visible or not
     */
    val chunkTracker: SimpleChunkTracker = SimpleChunkTracker()

    /**
     * Tracks random block things, like the last nether portal we used
     */
    val miscBlockTracker: MiscBlockTracker = MiscBlockTracker()

    // Renderers
    private val commandStatusOverlay: CommandStatusOverlay = CommandStatusOverlay()

    /**
     * AltoClef Settings
     */
    // Settings
    lateinit var modSettings: Settings
        private set

    /**
     * Sends chat messages (avoids auto-kicking)
     */
    // Misc managers/input
    val messageSender: MessageSender = MessageSender()

    /**
     * Manual control over input actions (ex. jumping, attacking)
     */
    val inputControls: InputControls = InputControls()

    /**
     * Does Inventory/container slot actions
     */
    val slotHandler: SlotHandler = SlotHandler()

    /**
     * Butler controller. Keeps track of users and lets you receive user messages
     */
    // Butler
    val butler: Butler = Butler()

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // As such, nothing will be loaded here but basic initialization.

        // This code should be run after Minecraft loads everything else in.
        // This is the actual start point, controlled by a mixin.
        initializeBaritoneSettings()

        // Central Managers

        // Task chains
        DeathMenuChain(taskRunner)
        PlayerInteractionFixChain(taskRunner)
        WorldSurvivalChain(taskRunner)

        // Misc managers
        initializeCommands()

        // Load settings
        Settings.load { newSettings: Settings ->
            modSettings = newSettings
            // Baritone's `acceptableThrowawayItems` should match our own.
            val baritoneCanPlace = modSettings.getThrowawayItems(true).filter { item ->
                !(item == Items.SOUL_SAND || item == Items.MAGMA_BLOCK || item == Items.SAND || item == Items.GRAVEL)
            }.toList()

            clientBaritoneSettings.acceptableThrowawayItems.value.addAll(baritoneCanPlace)
            // If we should run an idle command...
            if ((!userTaskChain.isActive || userTaskChain.isRunningIdleTask) && modSettings.shouldRunIdleCommandWhenNotActive()) {
                userTaskChain.signalNextTaskToBeIdleTask()
                commandExecutor.executeWithPrefix(modSettings.idleCommand)
            }
            // Don't break blocks or place blocks where we are explicitly protected.
            extraBaritoneSettings.avoidBlockBreak { blockPos: BlockPos? ->
                modSettings.isPositionExplicitlyProtected(blockPos)
            }
            extraBaritoneSettings.avoidBlockPlace { blockPos: BlockPos? ->
                modSettings.isPositionExplicitlyProtected(blockPos)
            }
        }

        ClientSendMessageEvents.ALLOW_CHAT.register { message ->
            if (commandExecutor.isClientCommand(message)) {
                commandExecutor.execute(message)
                false
            } else true
        }

        ClientTickEvents.START_CLIENT_TICK.register { onClientTick() }
        HudRenderCallback.EVENT.register { matrixStack, _ -> onClientRenderOverlay(matrixStack) }


        // External mod initialization
        runEnqueuedPostInits()
    }

    // Client tick
    private fun onClientTick() {
        runEnqueuedPostInits()
        inputControls.onTickPre()

        // Cancel shortcut
        if (InputHelper.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) && InputHelper.isKeyPressed(GLFW.GLFW_KEY_K)) {
            userTaskChain.cancel()
            if (taskRunner.currentTaskChain != null)
                taskRunner.currentTaskChain.stop()
        }

        // TODO: should this go here?
        itemStorage.setDirty()
        containerSubTracker.onServerTick()
        miscBlockTracker.tick()
        trackerManager.tick()
        blockTracker.preTickTask()
        taskRunner.tick()
        blockTracker.postTickTask()
        butler.tick()
        messageSender.tick()
        inputControls.onTickPost()
    }

    /// GETTERS AND SETTERS
    private fun onClientRenderOverlay(matrixStack: MatrixStack) {
        commandStatusOverlay.render(matrixStack)
    }

    private fun initializeBaritoneSettings() {
        extraBaritoneSettings.canWalkOnEndPortal(false)
        clientBaritoneSettings.freeLook.value = false
        clientBaritoneSettings.overshootTraverse.value = false
        clientBaritoneSettings.allowOvershootDiagonalDescend.value = true
        clientBaritoneSettings.allowInventory.value = true
        clientBaritoneSettings.allowParkour.value = false
        clientBaritoneSettings.allowParkourAscend.value = false
        clientBaritoneSettings.allowParkourPlace.value = false
        clientBaritoneSettings.allowDiagonalDescend.value = false
        clientBaritoneSettings.allowDiagonalAscend.value = false
        clientBaritoneSettings.blocksToAvoid.value = listOf(
            Blocks.FLOWERING_AZALEA, Blocks.AZALEA,
            Blocks.POWDER_SNOW, Blocks.BIG_DRIPLEAF, Blocks.BIG_DRIPLEAF_STEM, Blocks.CAVE_VINES,
            Blocks.CAVE_VINES_PLANT, Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT, Blocks.SWEET_BERRY_BUSH,
            Blocks.WARPED_ROOTS, Blocks.VINE, Blocks.GRASS, Blocks.FERN, Blocks.TALL_GRASS, Blocks.LARGE_FERN,
            Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD,
            Blocks.AMETHYST_CLUSTER, Blocks.SCULK, Blocks.SCULK_VEIN
        )
        // Let baritone move items to hotbar to use them
        // Reduces a bit of far rendering to save FPS
        clientBaritoneSettings.fadePath.value = true
        // Don't let baritone scan dropped items, we handle that ourselves.
        clientBaritoneSettings.mineScanDroppedItems.value = false
        // Don't let baritone wait for drops, we handle that ourselves.
        clientBaritoneSettings.mineDropLoiterDurationMSThanksLouca.value = 0L

        // Water bucket placement will be handled by us exclusively
        extraBaritoneSettings.configurePlaceBucketButDontFall(true)

        // For render smoothing
        clientBaritoneSettings.randomLooking.value = 0.0
        clientBaritoneSettings.randomLooking113.value = 0.0

        // Give baritone more time to calculate paths. Sometimes they can be really far away.
        // Was: 2000L
        clientBaritoneSettings.failureTimeoutMS.reset()
        // Was: 5000L
        clientBaritoneSettings.planAheadFailureTimeoutMS.reset()
        // Was 100
        clientBaritoneSettings.movementTimeoutTicks.reset()
    }

    // List all command sources here.
    private fun initializeCommands() {
        val manager = FabricClientCommandManager(
            AsyncCommandCoordinator.builder<ClientCommand>()
                .withAsynchronousParsing()
                .withExecutor(scheduledThreadPool)
                .build(),
            ::ClientCommand,
            ClientCommand::source
        )

        manager.parserRegistry().registerParserSupplier<ItemStackPredicateArgument, _> {
            FabricArgumentParsers.contextual(ItemPredicateArgumentType::itemPredicate)
        }

        MinecraftExceptionHandler<ClientCommand>()
            .withDefaultHandlers()
            .withDecorator { message: Component -> messagePrefix.append(message) }
            .apply(manager, AudienceProvider.nativeAudience<ClientCommand>())

        val annotationParser = AnnotationParser<ClientCommand>(manager) { SimpleCommandMeta.empty() }.apply {
            installCoroutineSupport()
        }

        val help = MinecraftHelp.createNative("altoclef help", manager)

        annotationParser.parseCommands(
            CoreCommands(help),
            UtilCommands(),
            InventoryCommands(),
            MiscCommands(),
            ResourceCommands(),
            SpeedrunCommands(),
        )

        try {
            // Legacy commands
            // TODO port these commands to cloud commands
            AltoClefCommands()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Baritone access (could just be static honestly)
     */
    val clientBaritone: Baritone
        get() = if (player == null) {
            BaritoneAPI.getProvider().primaryBaritone as Baritone
        } else BaritoneAPI.getProvider().getBaritoneForPlayer(player) as Baritone

    /**
     * Baritone settings access (could just be static honestly)
     */
    val clientBaritoneSettings: BaritoneSettings
        get() = Baritone.settings()

    /**
     * Baritone settings special to AltoClef (could just be static honestly)
     */
    val extraBaritoneSettings: AltoClefSettings
        get() = AltoClefSettings.getInstance()

    /**
     * Minecraft player client access (could just be static honestly)
     */
    val player: ClientPlayerEntity?
        get() = MinecraftClient.getInstance().player

    /**
     * Minecraft world access (could just be static honestly)
     */
    val world: ClientWorld?
        get() = MinecraftClient.getInstance().world

    /**
     * Minecraft client interaction controller access (could just be static honestly)
     */
    val controller: ClientPlayerInteractionManager?
        get() = MinecraftClient.getInstance().interactionManager

    /**
     * Run a user task
     */
    @JvmOverloads
    fun runUserTask(task: Task, onFinish: Runnable = Runnable {}) {
        userTaskChain.runTask(task, onFinish)
    }

    /**
     * Cancel currently running user task
     */
    fun cancelUserTask() {
        userTaskChain.cancel()
    }

    /**
     * Logs to the console and also messages any player using the bot as a butler.
     */
    @JvmOverloads
    fun log(message: String?, priority: MessagePriority? = MessagePriority.TIMELY) {
        Debug.logMessage(message)
        butler.onLog(message, priority)
    }

    /**
     * Logs a warning to the console and also alerts any player using the bot as a butler.
     */
    @JvmOverloads
    fun logWarning(message: String?, priority: MessagePriority? = MessagePriority.TIMELY) {
        Debug.logWarning(message)
        butler.onLogWarning(message, priority)
    }

    private fun runEnqueuedPostInits() {
        synchronized(postInitQueue) {
            while (!postInitQueue.isEmpty()) {
                postInitQueue.poll().accept(this)
            }
        }
    }

    private object AltoClefThreadFactory : ThreadFactory {
        private val threadGroup = currentThread.threadGroup
        private val threadCount = atomic(0)
        override fun newThread(runnable: Runnable): Thread =
            Thread(threadGroup, runnable, "AltoClef-Worker-${threadCount.getAndIncrement()}", 0)
    }
}
