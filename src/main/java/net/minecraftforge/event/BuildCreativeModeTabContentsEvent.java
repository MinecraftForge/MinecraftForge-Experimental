/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.util.MutableHashedLinkedMap;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.function.Supplier;

/**
 * Fired when the contents of a specific creative mode tab are being populated.
 * This event may be fired multiple times if the operator status of the local player or enabled feature flags changes.
 * <p>
 * This event is fired only on the {@linkplain LogicalSide#CLIENT logical client}.
 *
 * @param getTab the creative mode tab currently populating its contents
 * @param getTabKey the key of the creative mode tab currently populating its contents
 */
@NullMarked
public record BuildCreativeModeTabContentsEvent(
        CreativeModeTab getTab,
        ResourceKey<CreativeModeTab> getTabKey,
        CreativeModeTab.ItemDisplayParameters getParameters,
        MutableHashedLinkedMap<ItemStack, CreativeModeTab.TabVisibility> getEntries
) implements RecordEvent, CreativeModeTab.Output {
    public static final EventBus<BuildCreativeModeTabContentsEvent> BUS = EventBus.create(BuildCreativeModeTabContentsEvent.class);

    /** @deprecated {@link BuildCreativeModeTabContentsEvent} is no longer an {@link IModBusEvent}, so use {@link #BUS} directly. */
    @Deprecated(forRemoval = true, since = "1.21.9")
    public static EventBus<BuildCreativeModeTabContentsEvent> getBus(BusGroup modBusGroup) {
        return BUS;
    }

    @ApiStatus.Internal
    public BuildCreativeModeTabContentsEvent {}

    public FeatureFlagSet getFlags() {
        return getParameters.enabledFeatures();
    }

    public boolean hasPermissions() {
        return getParameters.hasPermissions();
    }

    @Override
    public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
        getEntries().put(stack, visibility);
    }

    public void accept(Supplier<? extends ItemLike> item, CreativeModeTab.TabVisibility visibility) {
        this.accept(item.get(), visibility);
    }

    public void accept(Supplier<? extends ItemLike> item) {
        this.accept(item.get());
    }
}
