/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity.living;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import org.jetbrains.annotations.Nullable;

/**
 * LivingEvent is fired whenever an event involving a {@link LivingEntity} occurs.
 */
public interface LivingEvent extends EntityEvent {
    EventBus<LivingEvent> BUS = EventBus.create(LivingEvent.class);

    @Override
    LivingEntity getEntity();

    /**
     * LivingUpdateEvent is fired when a LivingEntity is ticked in {@link LivingEntity#tick()}. <br>
     * <br>
     * This event is fired via the {@link ForgeEventFactory#onLivingTick(LivingEntity)}.<br>
     * <br>
     * This event is {@link Cancelable}.<br>
     * If this event is canceled, the Entity does not update.<br>
     * <br>
     * This event does not have a result. {@link HasResult}<br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
     **/
    record LivingTickEvent(LivingEntity getEntity) implements Cancellable, LivingEvent {
        public static final CancellableEventBus<LivingTickEvent> BUS = CancellableEventBus.create(LivingTickEvent.class);
    }

    /**
     * LivingJumpEvent is fired when an Entity jumps.<br>
     * This event is fired whenever an Entity jumps in
     * {@code LivingEntity#jumpFromGround()}, {@code MagmaCube#jumpFromGround()},
     * and {@code Horse#jumpFromGround()}.<br>
     * <br>
     * This event is fired via the {@link ForgeHooks#onLivingJump(LivingEntity)}.<br>
     * <br>
     * This event is not {@link Cancelable}.<br>
     * <br>
     * This event does not have a result. {@link HasResult}<br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
     **/
    record LivingJumpEvent(LivingEntity getEntity) implements LivingEvent {
        public static final EventBus<LivingJumpEvent> BUS = EventBus.create(LivingJumpEvent.class);
    }

    final class LivingVisibilityEvent implements LivingEvent {
        public static final EventBus<LivingVisibilityEvent> BUS = EventBus.create(LivingVisibilityEvent.class);

        private final LivingEntity livingEntity;
        private double visibilityModifier;
        @Nullable
        private final Entity lookingEntity;

        public LivingVisibilityEvent(LivingEntity livingEntity, @Nullable Entity lookingEntity, double originalMultiplier) {
            this.livingEntity = livingEntity;
            this.visibilityModifier = originalMultiplier;
            this.lookingEntity = lookingEntity;
        }

        @Override
        public LivingEntity getEntity() {
            return livingEntity;
        }

        /**
         * @param mod Is multiplied with the current modifier
         */
        public void modifyVisibility(double mod) {
            visibilityModifier *= mod;
        }

        /**
         * @return The current modifier
         */
        public double getVisibilityModifier() {
            return visibilityModifier;
        }

        /**
         * @return The entity trying to see this LivingEntity, if available
         */
        @Nullable
        public Entity getLookingEntity() {
            return lookingEntity;
        }
    }
}
