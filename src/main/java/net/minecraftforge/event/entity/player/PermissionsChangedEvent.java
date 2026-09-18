/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// This event will fire when the player is made a server operator ("opped") or stripped of such ("deopped").
///
/// This event is cancelable which will stop the op or deop from happening.
///
/// @param getEntity The player who is being oped or deopped.
/// @param getNewLevel The new permission level.
/// @param getOldLevel The old permission level.
@NullMarked
public record PermissionsChangedEvent(
        ServerPlayer getEntity,
        @Nullable LevelBasedPermissionSet getNewLevel,
        @Nullable LevelBasedPermissionSet getOldLevel
) implements Cancellable, PlayerEvent, RecordEvent {
    public static final CancellableEventBus<PermissionsChangedEvent> BUS = CancellableEventBus.create(PermissionsChangedEvent.class);

    @ApiStatus.Internal
    public PermissionsChangedEvent {}
}
