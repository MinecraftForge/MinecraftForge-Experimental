/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.event;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired after the player's movement inputs are updated.
 * <p>This event is fired only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
 *
 * @param getInput the player's movement inputs
 */
public record MovementInputUpdateEvent(Player getEntity, ClientInput getInput) implements RecordEvent, PlayerEvent {
    public static final EventBus<MovementInputUpdateEvent> BUS = EventBus.create(MovementInputUpdateEvent.class);

    @ApiStatus.Internal
    public MovementInputUpdateEvent {}
}
