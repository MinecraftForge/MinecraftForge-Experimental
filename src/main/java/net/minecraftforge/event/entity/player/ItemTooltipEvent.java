/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.player;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// This event is fired in {@link ItemStack#getTooltipLines(TooltipContext, Player, TooltipFlag)}, which in turn is
/// called from its respective Container.
///
/// Tooltips are also gathered with a null player during startup by {@link Minecraft#createSearchTrees()}.
///
/// @param getItemStack The [ItemStack] with the tooltip.
/// @param getEntity null during startup when populating search trees for tooltips
/// @param getToolTip The tooltip lines for the [ItemStack] that you can modify
/// @param getFlags Use to determine if the advanced information on item tooltips is being shown, toggled by F3+H.
/// @param getContext The [TooltipContext] for this tooltip.
/// @param getDisplay The [TooltipDisplay] for this tooltip.
public record ItemTooltipEvent(
        @NotNull ItemStack getItemStack,
        @Nullable Player getEntity,
        List<Component> getToolTip,
        TooltipFlag getFlags,
        TooltipContext getContext,
        TooltipDisplay getDisplay
) implements RecordEvent, PlayerEvent {
    public static final EventBus<ItemTooltipEvent> BUS = EventBus.create(ItemTooltipEvent.class);

    @ApiStatus.Internal
    public ItemTooltipEvent {}
}
