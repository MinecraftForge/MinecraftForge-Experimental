/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.village;

import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades.ItemListing;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import org.jspecify.annotations.NullMarked;

/**
 * VillagerTradesEvent is fired during the {@link ServerAboutToStartEvent}. It is used to gather the trade lists for each profession.
 * <p>It is fired once for each registered villager profession.</p>
 * <p>Villagers pick two trades from their trade map, based on their level.</p>
 * <p>Villager level is increased by successful trades.</p>
 * <p>The map is populated for levels 1-5 (inclusive), so getting a value will never return null for those keys.</p>
 * <p>Levels outside of this range do nothing, as specified by {@link VillagerData#canLevelUp(int)} which is called before attempting to level up.</p>
 * <p>To add trades to the merchant, simply add new trades to the list.</p>
 */
@NullMarked
public record VillagerTradesEvent(
        Int2ObjectMap<List<ItemListing>> getTrades,
        ResourceKey<VillagerProfession> getType
) implements RecordEvent {
    public static final EventBus<VillagerTradesEvent> BUS = EventBus.create(VillagerTradesEvent.class);
}
