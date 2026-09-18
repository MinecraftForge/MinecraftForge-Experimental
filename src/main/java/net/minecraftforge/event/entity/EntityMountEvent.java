/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import org.jspecify.annotations.Nullable;

/// This event gets fired whenever a entity mounts/dismounts another entity.
///
/// **entityBeingMounted can be null**, be sure to check for that.
///
/// This event is [Cancellable]. If this event is cancelled, the entity does not mount/dismount the other entity.
public record EntityMountEvent(
        Entity getEntityMounting,
        @Nullable Entity getEntityBeingMounted,
        Level getLevel,
        boolean isMounting
) implements Cancellable, EntityEvent, RecordEvent {
    public static final CancellableEventBus<EntityMountEvent> BUS = CancellableEventBus.create(EntityMountEvent.class);

    @Override
    public Entity getEntity() {
        return getEntityMounting;
    }

    public boolean isDismounting() {
        return !isMounting;
    }
}
