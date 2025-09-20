/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired before a selection highlight is rendered.
 * See the two subclasses to listen for blocks or entities.
 *
 * @see Block
 * @see Entity
 */
public sealed interface RenderHighlightEvent extends Cancellable, InheritableEvent {
    CancellableEventBus<RenderHighlightEvent> BUS = CancellableEventBus.create(RenderHighlightEvent.class);

    /**
     * {@return the level renderer}
     */
    LevelRenderer getLevelRenderer();

    /**
     * {@return the camera information}
     */
    Camera getCamera();

    /**
     * {@return the hit result which triggered the selection highlight}
     */
    HitResult getTarget();

    /**
     * {@return the partial tick}
     */
    float getPartialTick();

    /**
     * {@return the pose stack used for rendering}
     */
    PoseStack getPoseStack();

    /**
     * {@return the source of rendering buffers}
     */
    MultiBufferSource getMultiBufferSource();

    /**
     * Fired before a block's selection highlight is rendered.
     *
     * <p>This event is {@linkplain Cancellable cancellable}.
     * If the event is cancelled, then the selection highlight will not be rendered.</p>
     *
     * <p>This event is fired only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     */
    record Block(
            LevelRenderer getLevelRenderer,
            Camera getCamera,
            BlockHitResult getTarget,
            float getPartialTick,
            PoseStack getPoseStack,
            MultiBufferSource getMultiBufferSource
    ) implements RenderHighlightEvent {
        public static final CancellableEventBus<Block> BUS = CancellableEventBus.create(Block.class);

        @ApiStatus.Internal
        public Block {}

        /**
         * {@return the block hit result}
         */
        @Override
        public BlockHitResult getTarget() {
            return getTarget;
        }
    }

    /**
     * Fired before an entity's selection highlight is rendered.
     *
     * <p>This event is not {@linkplain Cancellable cancellable}.</p>
     *
     * <p>This event is fired only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     */
    record Entity(
            LevelRenderer getLevelRenderer,
            Camera getCamera,
            EntityHitResult getTarget,
            float getPartialTick,
            PoseStack getPoseStack,
            MultiBufferSource getMultiBufferSource
    ) implements RenderHighlightEvent {
        public static final CancellableEventBus<Entity> BUS = CancellableEventBus.create(Entity.class);

        @ApiStatus.Internal
        public Entity {}

        /**
         * {@return the entity hit result}
         */
        @Override
        public EntityHitResult getTarget() {
            return getTarget;
        }
    }
}
