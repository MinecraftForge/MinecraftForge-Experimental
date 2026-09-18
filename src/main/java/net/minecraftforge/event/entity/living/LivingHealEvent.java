/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.living;

import net.minecraftforge.event.ForgeEventFactory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import org.jspecify.annotations.NullMarked;

/// LivingHealEvent is fired when an Entity is set to be healed.
///
/// This event is fired whenever an Entity is healed in [LivingEntity#heal(float)]
///
/// This event is fired via the [ForgeEventFactory#onLivingHeal(LivingEntity, float)].
///
/// This event is [Cancellable]. If this event is cancelled, the Entity is not healed.
@NullMarked
public final class LivingHealEvent extends MutableEvent implements Cancellable, LivingEvent {
    public static final CancellableEventBus<LivingHealEvent> BUS = CancellableEventBus.create(LivingHealEvent.class);

    private final LivingEntity entity;
    private float amount;

    public LivingHealEvent(LivingEntity entity, float amount) {
        this.entity = entity;
        this.setAmount(amount);
    }

    @Override
    public LivingEntity getEntity() {
        return entity;
    }

    /// @return the amount of healing done to the Entity that was healed.
    public float getAmount() {
        return amount;
    }

    /// Sets the amount of healing done to the Entity that was healed.
    public void setAmount(float amount) {
        this.amount = amount;
    }
}
