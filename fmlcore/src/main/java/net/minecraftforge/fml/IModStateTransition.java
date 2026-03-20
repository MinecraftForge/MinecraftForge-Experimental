/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml;

import net.minecraftforge.fml.event.IModBusEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.minecraftforge.fml.loading.progress.ProgressMeter;
import org.jetbrains.annotations.Nullable;

public interface IModStateTransition {
    /** Magic value to allow me to optimize the futures list by ignoring the default value without making old methods nullable. */
    BiFunction<Executor, ? extends EventGenerator<?>, CompletableFuture<Void>> NULL_HOOK = (_, _) -> CompletableFuture.completedFuture(null);

    static IModStateTransition buildNoopTransition() {
        return ModStateTransitionHelper.NOOP;
    }

    default CompletableFuture<Void> build(
        final String name,
        final Executor syncExecutor,
        final Executor parallelExecutor,
        final ProgressMeter progressBar,
        final Function<Executor, CompletableFuture<Void>> preSyncTask,
        final Function<Executor, CompletableFuture<Void>> postSyncTask
    ) {
        return ModStateTransitionHelper.build(this, name, syncExecutor, parallelExecutor, progressBar, preSyncTask, postSyncTask);
    }

    default BiFunction<ModLoadingStage, Throwable, ModLoadingStage> nextModLoadingStage() {
        return ModLoadingStage::nextState;
    }

    /**
     * This used to allow you to fire multiple events during the transition. However, in doing so it would cause issues with the default
     * ModContainer's event handlers causing issues such as mod classes being constructed multiple times.
     */
    @Deprecated(since = "1.21.3", forRemoval = true)
    default Supplier<Stream<EventGenerator<?>>> eventFunctionStream() {
        return () -> Stream.ofNullable(eventFunction());
    }

    @Nullable
    default <T extends IModBusEvent> EventGenerator<T> eventFunction() {
        return null;
    }

    ThreadSelector threadSelector();
    BiFunction<Executor, CompletableFuture<Void>, CompletableFuture<Void>> finalActivityGenerator();

    interface EventGenerator<T extends IModBusEvent> extends Function<ModContainer, T> {
        static <FN extends IModBusEvent> EventGenerator<FN> fromFunction(Function<ModContainer, FN> fn) {
            return fn::apply;
        }
    }
}
