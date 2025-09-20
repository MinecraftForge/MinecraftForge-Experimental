/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import org.jspecify.annotations.NullMarked;

/**
 * This event will fire when the player is opped or deopped.
 * <p>
 * This event is cancelable which will stop the op or deop from happening.
 * @param getNewLevel The new permission level.
 * @param getOldLevel The old permission level.
 */
@NullMarked
public record PermissionsChangedEvent(ServerPlayer getEntity, int getNewLevel, int getOldLevel)
        implements Cancellable, PlayerEvent, RecordEvent {
    public static final CancellableEventBus<PermissionsChangedEvent> BUS = CancellableEventBus.create(PermissionsChangedEvent.class);
}
