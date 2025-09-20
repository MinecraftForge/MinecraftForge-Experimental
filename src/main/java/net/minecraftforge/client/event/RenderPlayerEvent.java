/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired when a player is being rendered.
 * See the two subclasses for listening for before and after rendering.
 *
 * @see RenderPlayerEvent.Pre
 * @see RenderPlayerEvent.Post
 * @see PlayerRenderer
 */
public sealed interface RenderPlayerEvent {
    PlayerRenderState getState();

    /**
     * {@return the player entity renderer}
     */
    PlayerRenderer getRenderer();

    /**
     * {@return the pose stack used for rendering}
     */
    PoseStack getPoseStack();

    /**
     * {@return the source of rendering buffers}
     */
    MultiBufferSource getMultiBufferSource();

    /**
     * {@return the amount of packed (sky and block) light for rendering}
     *
     * @see LightTexture
     */
    int getPackedLight();

    /**
     * Fired <b>before</b> the player is rendered.
     * This can be used for rendering additional effects or suppressing rendering.
     *
     * <p>This event is {@linkplain Cancellable cancellable}.
     * If this event is cancelled, then the player will not be rendered and the corresponding
     * {@link RenderPlayerEvent.Post} will not be fired.</p>
     *
     * <p>This event is fired only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     */
    record Pre(
            PlayerRenderState getState,
            PlayerRenderer getRenderer,
            PoseStack getPoseStack,
            MultiBufferSource getMultiBufferSource,
            int getPackedLight
    ) implements Cancellable, RecordEvent, RenderPlayerEvent {
        public static final CancellableEventBus<Pre> BUS = CancellableEventBus.create(Pre.class);

        @ApiStatus.Internal
        public Pre {}
    }

    /**
     * Fired <b>after</b> the player is rendered, if the corresponding {@link RenderPlayerEvent.Pre} is not cancelled.
     *
     * <p>This event is only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     */
    record Post(
            PlayerRenderState getState,
            PlayerRenderer getRenderer,
            PoseStack getPoseStack,
            MultiBufferSource getMultiBufferSource,
            int getPackedLight
    ) implements RecordEvent, RenderPlayerEvent {
        public static final EventBus<Post> BUS = EventBus.create(Post.class);

        @ApiStatus.Internal
        public Post {}
    }
}
