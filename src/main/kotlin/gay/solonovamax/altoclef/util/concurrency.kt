@file:Suppress("unused", "FunctionName")

package gay.solonovamax.altoclef.util


import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

fun ScheduledThreadPool(
    corePoolSize: Int,
    threadFactory: ThreadFactory? = null,
): ScheduledExecutorService {
    return if (threadFactory != null)
        Executors.newScheduledThreadPool(corePoolSize, threadFactory)
    else
        Executors.newScheduledThreadPool(corePoolSize)
}

fun FixedThreadPool(
    corePoolSize: Int,
    threadFactory: ThreadFactory? = null,
): ExecutorService {
    return if (threadFactory != null)
        Executors.newFixedThreadPool(corePoolSize, threadFactory)
    else
        Executors.newFixedThreadPool(corePoolSize)
}

fun ScheduledExecutorService.schedule(
    delay: Long,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    command: () -> Unit,
): ScheduledFuture<*> {
    return schedule(command, delay, unit)
}

fun ScheduledExecutorService.schedule(
    delay: Duration,
    command: () -> Unit,
): ScheduledFuture<*> {
    return schedule(delay.inWholeMilliseconds, TimeUnit.MILLISECONDS, command)
}

fun <V : Any> ScheduledExecutorService.schedule(
    delay: Long,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    callable: Callable<V>,
): ScheduledFuture<V> {
    return schedule(callable, delay, unit)
}

fun <V : Any> ScheduledExecutorService.schedule(
    delay: Duration,
    callable: Callable<V>,
): ScheduledFuture<V> {
    return schedule(delay.inWholeMilliseconds, TimeUnit.MILLISECONDS, callable)
}

fun ScheduledExecutorService.scheduleAtFixedRate(
    initialDelay: Long,
    period: Long,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    command: () -> Unit,
): ScheduledFuture<*> {
    return scheduleAtFixedRate(command, initialDelay, period, unit)
}

fun ScheduledExecutorService.scheduleAtFixedRate(
    initialDelay: Duration,
    period: Duration,
    command: () -> Unit,
): ScheduledFuture<*> {
    return scheduleAtFixedRate(
        initialDelay.inWholeMilliseconds,
        period.inWholeMilliseconds,
        TimeUnit.MILLISECONDS,
        command
    )
}

fun ScheduledExecutorService.fixedRate(
    initialDelay: Long,
    period: Long,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    command: () -> Unit,
): ScheduledFuture<*> {
    return scheduleAtFixedRate(initialDelay, period, unit, command)
}

fun ScheduledExecutorService.fixedRate(
    initialDelay: Duration,
    period: Duration,
    command: () -> Unit,
): ScheduledFuture<*> {
    return fixedRate(initialDelay.inWholeMilliseconds, period.inWholeMilliseconds, TimeUnit.MILLISECONDS, command)
}

fun ScheduledExecutorService.scheduleWithFixedDelay(
    initialDelay: Long,
    delay: Long,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    command: () -> Unit,
): ScheduledFuture<*> {
    return scheduleWithFixedDelay(command, initialDelay, delay, unit)
}

fun ScheduledExecutorService.scheduleWithFixedDelay(
    initialDelay: Duration,
    delay: Duration,
    command: () -> Unit,
): ScheduledFuture<*> {
    return scheduleWithFixedDelay(
        initialDelay.inWholeMilliseconds,
        delay.inWholeMilliseconds,
        TimeUnit.MILLISECONDS,
        command
    )
}

fun ScheduledExecutorService.fixedDelay(
    initialDelay: Long,
    delay: Long,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    command: () -> Unit,
): ScheduledFuture<*> {
    return scheduleWithFixedDelay(initialDelay, delay, unit, command)
}

fun ScheduledExecutorService.fixedDelay(
    initialDelay: Duration,
    delay: Duration,
    command: () -> Unit,
): ScheduledFuture<*> {
    return fixedDelay(initialDelay.inWholeMilliseconds, delay.inWholeMilliseconds, TimeUnit.MILLISECONDS, command)
}

operator fun Runnable.invoke(): Unit = run()

inline val currentThread: Thread
    get() = Thread.currentThread()
