@file:Suppress("unused", "FunctionName")

package gay.solonovamax.altoclef.util

import cloud.commandframework.CommandManager
import cloud.commandframework.annotations.AnnotationAccessor
import cloud.commandframework.annotations.AnnotationParser
import cloud.commandframework.annotations.injection.ParameterInjectorRegistry
import cloud.commandframework.arguments.parser.ArgumentParser
import cloud.commandframework.arguments.parser.ParserParameters
import cloud.commandframework.arguments.parser.ParserRegistry
import cloud.commandframework.context.CommandContext
import cloud.commandframework.execution.postprocessor.CommandPostprocessor
import cloud.commandframework.execution.preprocessor.CommandPreprocessor
import cloud.commandframework.meta.CommandMeta
import cloud.commandframework.types.tuples.Triplet
import io.leangen.geantyref.TypeToken
import net.minecraft.item.Items
import cloud.commandframework.types.tuples.Pair as CloudPair

fun <C> AnnotationParser<C>.parseCommands(vararg instances: Any) {
    for (instance in instances)
        parse(instance)
}

fun <C> CommandManager<C>.registerCommandPreProcessors(vararg preProcessors: CommandPreprocessor<C>) {
    for (preProcessor in preProcessors)
        registerCommandPreProcessor(preProcessor)
}

fun <C> CommandManager<C>.registerCommandPostProcessors(vararg postProcessors: CommandPostprocessor<C>) {
    for (postProcessor in postProcessors)
        registerCommandPostProcessor(postProcessor)
}

inline fun <reified T, C> ParserRegistry<C>.registerParserSupplier(noinline supplier: (ParserParameters) -> ArgumentParser<C, T>) {
    registerParserSupplier(object : TypeToken<T>() {}, supplier)
}

inline fun <reified T, C> ParserRegistry<C>.registerParserSupplier(supplier: ArgumentParser<C, T>) {
    registerParserSupplier(object : TypeToken<T>() {}) { supplier }
}

inline fun <reified T, C> ParameterInjectorRegistry<C>.registerInjector(
    noinline function: (context: CommandContext<C>, annotationAccessor: AnnotationAccessor) -> T,
) {
    Items.OAK_PLANKS
    registerInjector(T::class.java, function)
}

inline fun <reified T> AnnotationParser(
    manager: CommandManager<T>,
    noinline metaMapper: (ParserParameters) -> CommandMeta
): AnnotationParser<T> {
    return AnnotationParser(manager, T::class.java, metaMapper)
}

operator fun <U, V, W> Triplet<U, V, W>.component3(): W = this.third
operator fun <U, V, W> Triplet<U, V, W>.component2(): V = this.second
operator fun <U, V, W> Triplet<U, V, W>.component1(): U = this.first

operator fun <U, V> CloudPair<U, V>.component1(): U = this.first
operator fun <U, V> CloudPair<U, V>.component2(): V = this.second
