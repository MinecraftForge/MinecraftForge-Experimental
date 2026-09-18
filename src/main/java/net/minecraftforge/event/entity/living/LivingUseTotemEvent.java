/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.living;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus;

/// Fired when an Entity attempts to use a totem to prevent its death.
///
/// This event is [Cancellable].
/// If this event is cancelled, the totem will not prevent the entity's death.
///
/// This event is fired only on the [logical server][LogicalSide#SERVER].
///
/// @param getSource The damage source that caused the entity to die
/// @param getTotem The totem of undying being used from the entity's inventory
/// @param getHandHolding The hand holding the totem
public record LivingUseTotemEvent(
        LivingEntity getEntity,
        DamageSource getSource,
        ItemStack getTotem,
        InteractionHand getHandHolding
) implements Cancellable, LivingEvent, RecordEvent {
    public static final CancellableEventBus<LivingUseTotemEvent> BUS = CancellableEventBus.create(LivingUseTotemEvent.class);

    @ApiStatus.Internal
    public LivingUseTotemEvent {}
}
