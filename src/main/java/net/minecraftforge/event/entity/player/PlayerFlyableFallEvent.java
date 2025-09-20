/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.player;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.bus.EventBus;
import org.jspecify.annotations.NullMarked;

/**
 * Occurs when a player falls, but is able to fly.  Doesn't need to be cancelable, this is mainly for notification purposes.
 * @author Mithion
 */
@NullMarked
public final class PlayerFlyableFallEvent implements PlayerEvent {
    public static final EventBus<PlayerFlyableFallEvent> BUS = EventBus.create(PlayerFlyableFallEvent.class);

    private final Player player;
    private float distance;
    private float multiplier;

    public PlayerFlyableFallEvent(Player player, float distance, float multiplier) {
        this.player = player;
        this.distance = distance;
        this.multiplier = multiplier;
    }

    @Override
    public Player getEntity() {
        return player;
    }

    public float getDistance() { return distance;}
    public void setDistance(float distance) { this.distance = distance; }
    public float getMultiplier() { return multiplier; }
    public void setMultiplier(float multiplier) { this.multiplier = multiplier; }
}
