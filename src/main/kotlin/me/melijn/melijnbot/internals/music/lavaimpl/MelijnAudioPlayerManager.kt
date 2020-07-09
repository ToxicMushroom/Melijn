package me.melijn.melijnbot.internals.music.lavaimpl

import com.sedmelluq.discord.lavaplayer.player.*
import com.sedmelluq.discord.lavaplayer.remote.RemoteNodeRegistry
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.ProbingAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.GarbageCollectionMonitor
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import com.sedmelluq.discord.lavaplayer.track.*
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import com.sedmelluq.lava.common.tools.DaemonThreadFactory
import com.sedmelluq.lava.common.tools.ExecutorTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import me.melijn.melijnbot.internals.music.SuspendingAudioLoadResultHandler
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.function.Function
import kotlin.experimental.and

open class MelijnAudioPlayerManager : AudioPlayerManager {
    private val sourceManagers: MutableList<AudioSourceManager>

    @Volatile
    private var httpConfigurator: Function<RequestConfig, RequestConfig>? = null

    @Volatile
    private var httpBuilderConfigurator: Consumer<HttpClientBuilder>? = null

    // Executors
    val executor: ExecutorService
    private val trackInfoExecutorService: ThreadPoolExecutor
    private val scheduledExecutorService: ScheduledExecutorService
    private val orderedInfoExecutor: MelijnOrderedExecutor

    // Configuration
    @Volatile
    var trackStuckThresholdNanos: Long
        private set

    private val configuration: AudioConfiguration
    private val cleanupThreshold: AtomicLong

    @Volatile
    private var frameBufferDuration: Int

    @Volatile
    private var useSeekGhosting: Boolean

    // Additional services
    private val garbageCollectionMonitor: GarbageCollectionMonitor
    private val lifecycleManager: AudioPlayerLifecycleManager
    override fun shutdown() {
        garbageCollectionMonitor.disable()
        lifecycleManager.shutdown()
        for (sourceManager in sourceManagers) {
            sourceManager.shutdown()
        }
        ExecutorTools.shutdownExecutor(executor, "track playback")
        ExecutorTools.shutdownExecutor(trackInfoExecutorService, "track info")
        ExecutorTools.shutdownExecutor(scheduledExecutorService, "scheduled operations")
    }

    override fun useRemoteNodes(vararg nodeAddresses: String) {

    }

    override fun enableGcMonitoring() {
        garbageCollectionMonitor.enable()
    }

    override fun registerSourceManager(sourceManager: AudioSourceManager) {
        sourceManagers.add(sourceManager)
        if (sourceManager is HttpConfigurable) {
            val configurator = httpConfigurator
            if (configurator != null) {
                (sourceManager as HttpConfigurable).configureRequests(configurator)
            }
            val builderConfigurator = httpBuilderConfigurator
            if (builderConfigurator != null) {
                (sourceManager as HttpConfigurable).configureBuilder(builderConfigurator)
            }
        }
    }

    override fun <T : AudioSourceManager?> source(klass: Class<T>): T? {
        for (sourceManager in sourceManagers) {
            if (klass.isAssignableFrom(sourceManager.javaClass)) {
                return sourceManager as T
            }
        }
        return null
    }

    override fun loadItem(identifier: String?, resultHandler: AudioLoadResultHandler?): Future<Void> {
        throw IllegalArgumentException("ugly, don't call")
    }

    override fun loadItemOrdered(orderingKey: Any?, identifier: String?, resultHandler: AudioLoadResultHandler?): Future<Void> {
        throw IllegalArgumentException("ugly, don't call")
    }

    suspend fun loadItem(identifier: String, resultHandler: SuspendingAudioLoadResultHandler) {
        try {
            CoroutineScope(trackInfoExecutorService.asCoroutineDispatcher()).launch {
                createItemLoader(identifier, resultHandler)
            }
        } catch (e: RejectedExecutionException) {
            handleLoadRejected(identifier, resultHandler, e)
        }
    }

    suspend fun loadItemOrdered(orderingKey: Any, identifier: String, resultHandler: SuspendingAudioLoadResultHandler) {
        try {
            orderedInfoExecutor.submit(orderingKey) {
                createItemLoader(identifier, resultHandler)
            }
        } catch (e: RejectedExecutionException) {
            handleLoadRejected(identifier, resultHandler, e)
        }
    }

    class CompletedVoidFuture : Future<Void?> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            return false
        }

        override fun isCancelled(): Boolean {
            return false
        }

        override fun isDone(): Boolean {
            return true
        }

        @Throws(InterruptedException::class, ExecutionException::class)
        override fun get(): Void? {
            return null
        }

        @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
        override fun get(timeout: Long, unit: TimeUnit): Void? {
            return null
        }
    }

    private suspend fun handleLoadRejected(identifier: String, resultHandler: SuspendingAudioLoadResultHandler, e: RejectedExecutionException) {
        val exception = FriendlyException("Cannot queue loading a track, queue is full.", FriendlyException.Severity.SUSPICIOUS, e)
        ExceptionTools.log(log, exception, "queueing item $identifier")
        resultHandler.loadFailed(exception)
    }

    private suspend fun createItemLoader(identifier: String, resultHandler: SuspendingAudioLoadResultHandler) {
        val reported = BooleanArray(1)
        try {
            if (!checkSourcesForItem(AudioReference(identifier, null), resultHandler, reported)) {
                log.debug("No matches for track with identifier {}.", identifier)
                resultHandler.noMatches()
            }
        } catch (throwable: Throwable) {
            if (reported[0]) {
                log.warn("Load result handler for {} threw an exception", identifier, throwable)
            } else {
                dispatchItemLoadFailure(identifier, resultHandler, throwable)
            }
            ExceptionTools.rethrowErrors(throwable)
        }
    }

    private suspend fun dispatchItemLoadFailure(identifier: String, resultHandler: SuspendingAudioLoadResultHandler, throwable: Throwable) {
        val exception = ExceptionTools.wrapUnfriendlyExceptions("Something went wrong when looking up the track", FriendlyException.Severity.FAULT, throwable)
        ExceptionTools.log(log, exception, "loading item $identifier")
        resultHandler.loadFailed(exception)
    }

    @Throws(IOException::class)
    override fun encodeTrack(stream: MessageOutput, track: AudioTrack) {
        val output = stream.startMessage()
        output.write(TRACK_INFO_VERSION)
        val trackInfo = track.info
        output.writeUTF(trackInfo.title)
        output.writeUTF(trackInfo.author)
        output.writeLong(trackInfo.length)
        output.writeUTF(trackInfo.identifier)
        output.writeBoolean(trackInfo.isStream)
        DataFormatTools.writeNullableText(output, trackInfo.uri)
        encodeTrackDetails(track, output)
        output.writeLong(track.position)
        stream.commitMessage(TRACK_INFO_VERSIONED)
    }

    @Throws(IOException::class)
    override fun decodeTrack(stream: MessageInput): DecodedTrackHolder? {
        val input = stream.nextMessage() ?: return null
        val version = if (stream.messageFlags and TRACK_INFO_VERSIONED != 0) {
            (input.readByte().and(0xFF.toByte()))
        } else {
            1
        }
        val trackInfo = AudioTrackInfo(input.readUTF(), input.readUTF(), input.readLong(), input.readUTF(),
            input.readBoolean(), if (version >= 2) DataFormatTools.readNullableText(input) else null)
        val track = decodeTrackDetails(trackInfo, input)
        val position = input.readLong()
        if (track != null) {
            track.position = position
        }
        stream.skipRemainingBytes()
        return DecodedTrackHolder(track)
    }

    /**
     * Encodes an audio track to a byte array. Does not include AudioTrackInfo in the buffer.
     * @param track The track to encode
     * @return The bytes of the encoded data
     */
    fun encodeTrackDetails(track: AudioTrack): ByteArray {
        return try {
            val byteOutput = ByteArrayOutputStream()
            val output: DataOutput = DataOutputStream(byteOutput)
            encodeTrackDetails(track, output)
            byteOutput.toByteArray()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    private fun encodeTrackDetails(track: AudioTrack, output: DataOutput) {
        val sourceManager = track.sourceManager
        output.writeUTF(sourceManager.sourceName)
        sourceManager.encodeTrack(track, output)
    }

    /**
     * Decodes an audio track from a byte array.
     * @param trackInfo Track info for the track to decode
     * @param buffer Byte array containing the encoded track
     * @return Decoded audio track
     */
    fun decodeTrackDetails(trackInfo: AudioTrackInfo, buffer: ByteArray?): AudioTrack? {
        return try {
            val input: DataInput = DataInputStream(ByteArrayInputStream(buffer))
            decodeTrackDetails(trackInfo, input)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    private fun decodeTrackDetails(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack? {
        val sourceName = input.readUTF()
        for (sourceManager in sourceManagers) {
            if (sourceName == sourceManager.sourceName) {
                return sourceManager.decodeTrack(trackInfo, input)
            }
        }
        return null
    }

    /**
     * Executes an audio track with the given player and volume.
     * @param listener A listener for track state events
     * @param track The audio track to execute
     * @param configuration The audio configuration to use for executing
     * @param playerOptions Options of the audio player
     */
    fun executeTrack(listener: TrackStateListener?, track: InternalAudioTrack, configuration: AudioConfiguration,
                     playerOptions: AudioPlayerOptions) {
        val executor = createExecutorForTrack(track, configuration, playerOptions)
        track.assignExecutor(executor, true)
        executor.execute(listener)
    }

    private fun createExecutorForTrack(track: InternalAudioTrack, configuration: AudioConfiguration,
                                       playerOptions: AudioPlayerOptions): AudioTrackExecutor {

        val customExecutor = track.createLocalExecutor(this)
        return if (customExecutor != null) {
            customExecutor
        } else {

            val bufferDuration = Optional.ofNullable(playerOptions.frameBufferDuration.get()).orElse(frameBufferDuration)
            LocalAudioTrackExecutor(track, configuration, playerOptions, useSeekGhosting, bufferDuration)
        }
    }

    override fun getConfiguration(): AudioConfiguration {
        return configuration
    }

    override fun isUsingSeekGhosting(): Boolean {
        return useSeekGhosting
    }

    override fun setUseSeekGhosting(useSeekGhosting: Boolean) {
        this.useSeekGhosting = useSeekGhosting
    }

    override fun getFrameBufferDuration(): Int {
        return frameBufferDuration
    }

    override fun setFrameBufferDuration(frameBufferDuration: Int) {
        this.frameBufferDuration = 200.coerceAtLeast(frameBufferDuration)
    }

    override fun setTrackStuckThreshold(trackStuckThreshold: Long) {
        trackStuckThresholdNanos = TimeUnit.MILLISECONDS.toNanos(trackStuckThreshold)
    }

    override fun setPlayerCleanupThreshold(cleanupThreshold: Long) {
        this.cleanupThreshold.set(cleanupThreshold)
    }

    override fun setItemLoaderThreadPoolSize(poolSize: Int) {
        trackInfoExecutorService.maximumPoolSize = poolSize
    }

    private suspend fun checkSourcesForItem(reference: AudioReference, resultHandler: SuspendingAudioLoadResultHandler, reported: BooleanArray): Boolean {
        var currentReference = reference
        var redirects = 0
        while (redirects < MAXIMUM_LOAD_REDIRECTS && currentReference.identifier != null) {
            val item = checkSourcesForItemOnce(currentReference, resultHandler, reported)
            if (item == null) {
                return false
            } else if (item !is AudioReference) {
                return true
            }
            currentReference = item
            redirects++
        }
        return false
    }

    private suspend fun checkSourcesForItemOnce(reference: AudioReference, resultHandler: SuspendingAudioLoadResultHandler, reported: BooleanArray): AudioItem? {
        for (sourceManager in sourceManagers) {
            if (reference.containerDescriptor != null && sourceManager !is ProbingAudioSourceManager) {
                continue
            }
            val item = sourceManager.loadItem(this, reference)

            if (item != null) {
                if (item is AudioTrack) {
                    log.debug("Loaded a track with identifier {} using {}.", reference.identifier, sourceManager.javaClass.simpleName)
                    reported[0] = true
                    resultHandler.trackLoaded(item)
                } else if (item is AudioPlaylist) {
                    log.debug("Loaded a playlist with identifier {} using {}.", reference.identifier, sourceManager.javaClass.simpleName)
                    reported[0] = true
                    resultHandler.playlistLoaded(item)
                }
                return item
            }
        }
        return null
    }

    override fun createPlayer(): AudioPlayer {
        val player = constructPlayer()
        player.addListener(lifecycleManager)
        return player
    }

    private fun constructPlayer(): AudioPlayer {
        return MelijnAudioPlayer(this)
    }

    override fun getRemoteNodeRegistry(): RemoteNodeRegistry {
        throw IllegalArgumentException("ANGRY")
    }

    override fun setHttpRequestConfigurator(configurator: Function<RequestConfig, RequestConfig>) {
        httpConfigurator = configurator
        for (sourceManager in sourceManagers) {
            if (sourceManager is HttpConfigurable) {
                (sourceManager as HttpConfigurable).configureRequests(configurator)
            }
        }
    }

    override fun setHttpBuilderConfigurator(configurator: Consumer<HttpClientBuilder>) {
        httpBuilderConfigurator = configurator
        for (sourceManager in sourceManagers) {
            if (sourceManager is HttpConfigurable) {
                (sourceManager as HttpConfigurable).configureBuilder(configurator)
            }
        }
    }

    companion object {
        private const val TRACK_INFO_VERSIONED = 1
        private const val TRACK_INFO_VERSION = 2
        private val DEFAULT_FRAME_BUFFER_DURATION = TimeUnit.SECONDS.toMillis(5).toInt()
        private val DEFAULT_CLEANUP_THRESHOLD = TimeUnit.MINUTES.toMillis(1).toInt()
        private const val MAXIMUM_LOAD_REDIRECTS = 5
        private const val DEFAULT_LOADER_POOL_SIZE = 10
        private const val LOADER_QUEUE_CAPACITY = 5000
        private val log = LoggerFactory.getLogger(DefaultAudioPlayerManager::class.java)
    }

    /**
     * Create a new instance
     */
    init {
        sourceManagers = ArrayList()

        // Executors
        executor = ThreadPoolExecutor(1, Int.MAX_VALUE, 10, TimeUnit.SECONDS,
            SynchronousQueue(), DaemonThreadFactory("playback"))
        trackInfoExecutorService = ExecutorTools.createEagerlyScalingExecutor(1, DEFAULT_LOADER_POOL_SIZE,
            TimeUnit.SECONDS.toMillis(30), LOADER_QUEUE_CAPACITY, DaemonThreadFactory("info-loader"))
        scheduledExecutorService = Executors.newScheduledThreadPool(1, DaemonThreadFactory("manager"))
        orderedInfoExecutor = MelijnOrderedExecutor(trackInfoExecutorService)

        // Configuration
        trackStuckThresholdNanos = TimeUnit.MILLISECONDS.toNanos(10000)
        configuration = AudioConfiguration()
        cleanupThreshold = AtomicLong(DEFAULT_CLEANUP_THRESHOLD.toLong())
        frameBufferDuration = DEFAULT_FRAME_BUFFER_DURATION
        useSeekGhosting = true

        // Additional services
        garbageCollectionMonitor = GarbageCollectionMonitor(scheduledExecutorService)
        lifecycleManager = AudioPlayerLifecycleManager(scheduledExecutorService, cleanupThreshold)
        lifecycleManager.initialise()
    }
}