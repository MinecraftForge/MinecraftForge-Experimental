/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.player;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.fml.LogicalSide;

/// Base interface shared among advancement-related events
///
/// @see AdvancementEarnEvent
/// @see AdvancementProgressEvent
public sealed interface AdvancementEvent extends PlayerEvent {
    AdvancementHolder getAdvancement();

    /// Fired when the player earns an advancement. An advancement is earned once its requirements are complete.
    ///
    /// Note that advancements may be hidden from the player or used in background mechanics, such as recipe
    /// advancements for unlocking recipes in the recipe book.
    ///
    /// This event is fired only on the [logical server][LogicalSide#SERVER].
    ///
    /// @see AdvancementProgress#isDone()
    record AdvancementEarnEvent(Player getEntity, AdvancementHolder getAdvancement)
            implements RecordEvent, AdvancementEvent {
        public static final EventBus<AdvancementEarnEvent> BUS = EventBus.create(AdvancementEarnEvent.class);
    }

    /// Fired when the player's progress on an advancement criterion is granted or revoked.
    ///
    /// This event is fired only on the [logical server][LogicalSide#SERVER].
    ///
    /// @param getAdvancementProgress the progress of the advancement
    /// @param getCriterionName the name of the criterion that was progressed
    /// @param getProgressType the type of progress for the criterion in this event
    ///
    /// @see AdvancementEarnEvent
    /// @see net.minecraft.server.PlayerAdvancements#award(AdvancementHolder, String)
    /// @see net.minecraft.server.PlayerAdvancements#revoke(AdvancementHolder, String)
    record AdvancementProgressEvent(
            Player getEntity,
            AdvancementHolder getAdvancement,
            AdvancementProgress getAdvancementProgress,
            String getCriterionName,
            ProgressType getProgressType
    ) implements RecordEvent, AdvancementEvent {
        public static final EventBus<AdvancementProgressEvent> BUS = EventBus.create(AdvancementProgressEvent.class);

        public enum ProgressType {
            GRANT, REVOKE
        }
    }
}
