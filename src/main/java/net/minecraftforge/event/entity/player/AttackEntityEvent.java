/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

/// AttackEntityEvent is fired when a player attacks an Entity in [Player#attack(Entity)].
///
/// This event is [cancellable][Cancellable]. If this event is cancelled, the player does not attack the Entity.
///
/// @param getEntity the player that attacked the entity
/// @param getTarget the entity that was attacked by the player
public record AttackEntityEvent(Player getEntity, Entity getTarget) implements Cancellable, PlayerEvent, RecordEvent {
    public static final CancellableEventBus<AttackEntityEvent> BUS = CancellableEventBus.create(AttackEntityEvent.class);
}
