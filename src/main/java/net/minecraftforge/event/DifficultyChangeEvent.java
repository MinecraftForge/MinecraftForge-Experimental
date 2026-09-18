/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event;

import net.minecraft.world.Difficulty;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import org.jspecify.annotations.NullMarked;

/// This event is fired when difficulty is changing.
///
/// This event is fired from [ForgeEventFactory#onDifficultyChange(Difficulty, Difficulty)].
@NullMarked
public record DifficultyChangeEvent(Difficulty getDifficulty, Difficulty getOldDifficulty) implements RecordEvent {
    public static final EventBus<DifficultyChangeEvent> BUS = EventBus.create(DifficultyChangeEvent.class);
}
