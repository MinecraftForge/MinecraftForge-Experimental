/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

/**
 * EntityTravelToDimensionEvent is fired before an Entity travels to a dimension.<br>
 * <br>
 * This event is {@linkplain Cancellable cancellable}.<br>
 * If this event is cancelled, the Entity does not travel to the dimension.
 *
 * @param getDimension the dimension the entity is travelling to
 */
public record EntityTravelToDimensionEvent(Entity getEntity, ResourceKey<Level> getDimension)
        implements Cancellable, EntityEvent, RecordEvent {
    public static final CancellableEventBus<EntityTravelToDimensionEvent> BUS = CancellableEventBus.create(EntityTravelToDimensionEvent.class);
}
