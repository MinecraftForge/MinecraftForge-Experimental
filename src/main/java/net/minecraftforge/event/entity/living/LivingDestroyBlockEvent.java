/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.living;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

/**
 * Fired when the ender dragon or wither attempts to destroy a block and when ever a zombie attempts to break a door. Basically a event version of {@link Block#canEntityDestroy(BlockState, BlockGetter, BlockPos, Entity)}<br>
 * <br>
 * This event is {@link Cancelable}.<br>
 * If this event is canceled, the block will not be destroyed.<br>
 **/
public record LivingDestroyBlockEvent(
        LivingEntity getEntity,
        BlockState getState,
        BlockPos getPos
) implements Cancellable, LivingEvent {
    public static final CancellableEventBus<LivingDestroyBlockEvent> BUS = CancellableEventBus.create(LivingDestroyBlockEvent.class);
}
