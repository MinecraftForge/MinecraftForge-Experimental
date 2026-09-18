/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity;

import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;

/// EntityEvent is a marker interface for when an event involving any Entity occurs.
public interface EntityEvent {
    /// @return the entity that caused this event to occur.
    Entity getEntity();

    /// EntityConstructing is fired when an Entity is being created.
    ///
    /// This event is fired within the constructor of the Entity.
    record EntityConstructing(Entity getEntity) implements RecordEvent, EntityEvent {
        public static final EventBus<EntityConstructing> BUS = EventBus.create(EntityConstructing.class);
    }

    /// This event is fired on server and client after an Entity has entered a different section.
    ///
    /// Sections are 16x16x16 block grids of the world.
    ///
    /// This event does not fire when a new entity is spawned, only when an entity moves from one section to another one.
    /// Use [EntityJoinLevelEvent] to detect new entities joining the world.
    ///
    /// @param getPackedOldPos the packed position of the old section's position. This is to be used with the various
    ///                        methods in [SectionPos], such as [SectionPos#of(long)] or [SectionPos#x(long)] to avoid
    ///                        allocation.
    /// @param getPackedNewPos the packed position of the new section's position. This is to be used with the various
    ///                        methods in [SectionPos], such as [SectionPos#of(long)] or [SectionPos#x(long)] to avoid
    ///                        allocation.
    record EnteringSection(Entity getEntity, long getPackedOldPos, long getPackedNewPos)
            implements RecordEvent, EntityEvent {
        public static final EventBus<EnteringSection> BUS = EventBus.create(EnteringSection.class);

        /// @return the position of the old section
        public SectionPos getOldPos() {
            return SectionPos.of(getPackedOldPos);
        }

        /// @return the position of the new section
        public SectionPos getNewPos() {
            return SectionPos.of(getPackedNewPos);
        }

        /// Whether the chunk has changed as part of this event. If this method returns false, only the Y position of
        /// the section has changed.
        public boolean didChunkChange() {
            return SectionPos.x(getPackedOldPos) != SectionPos.x(getPackedNewPos)
                    || SectionPos.z(getPackedOldPos) != SectionPos.z(getPackedNewPos);
        }
    }
}
