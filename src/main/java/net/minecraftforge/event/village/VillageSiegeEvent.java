/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.village;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

/// VillageSiegeEvent is fired just before a zombie siege finds a successful location, to give mods the chance to stop
/// the siege.
///
/// This event is [Cancellable]. Cancelling this event stops the siege.
public record VillageSiegeEvent(
        VillageSiege getSiege,
        Level getLevel,
        Player getPlayer,
        Vec3 getAttemptedSpawnPos
) implements Cancellable, RecordEvent {
    public static final CancellableEventBus<VillageSiegeEvent> BUS = CancellableEventBus.create(VillageSiegeEvent.class);
}
