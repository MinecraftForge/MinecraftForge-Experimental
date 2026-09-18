/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import org.jetbrains.annotations.Nullable;

/// This event is fired when a player's spawn point is set or reset.
///
/// This event is [Cancellable]. Cancelling will prevent the spawn point from being changed.
///
/// @param getEntity The player whose spawn point is being set or reset.
/// @param getConfig The config for the player respawn, or null if the spawn point is being reset.
public record PlayerSetSpawnEvent(Player getEntity, ServerPlayer.@Nullable RespawnConfig getConfig)
        implements Cancellable, RecordEvent, PlayerEvent {
    public static final CancellableEventBus<PlayerSetSpawnEvent> BUS = CancellableEventBus.create(PlayerSetSpawnEvent.class);
}
