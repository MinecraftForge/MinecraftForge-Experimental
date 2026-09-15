/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.event;

import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.IRenderCallback;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Fired before a block texture will be overlaid on the player's view.
 *
 * <p>This event is fired only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
 */
public abstract sealed class RenderBlockScreenEffectEvent extends MutableEvent implements Cancellable permits
    RenderBlockScreenEffectEvent.FireOverlayEvent,
    RenderBlockScreenEffectEvent.BlockOverlayEvent,
    RenderBlockScreenEffectEvent.WaterOverlayEvent
{
    private final Player player;
    private final BlockState blockState;
    private final BlockPos blockPos;
    private @Nullable IRenderCallback renderer;

    @ApiStatus.Internal
    protected RenderBlockScreenEffectEvent(Player player, BlockState blockState, BlockPos blockPos) {
        this.player = player;
        this.blockState = blockState;
        this.blockPos = blockPos;
    }

    /** The player which the overlay will apply to */
    public Player getPlayer() { return this.player; }

    /** The block which the overlay is gotten from */
    public BlockState getBlockSate() { return this.blockState; }

    /** The position of the block which the overlay is gotten from */
    public BlockPos getBlockPos() { return this.blockPos; }

    /** Returns the custom renderer, if any, that an event handler has registered for this event. */
    public @Nullable IRenderCallback getCustomRenderer() {
        return this.renderer;
    }

    /** Registers a custom renderer for this event, combine with canceling the event to take effect */
    public void setCustomRenderer(IRenderCallback renderer) {
        this.renderer = renderer;
    }


    /**
     * Fired when the player is burning / on fire.
     */
    public static final class FireOverlayEvent extends RenderBlockScreenEffectEvent {
        public static final CancellableEventBus<FireOverlayEvent> BUS = CancellableEventBus.create(FireOverlayEvent.class);

        @ApiStatus.Internal
        public FireOverlayEvent(Player player, BlockState blockState, BlockPos blockPos) {
            super(player, blockState, blockPos);
        }
    }

    /**
     * Fired when the player is suffocating inside a solid block.
     */
    public static final class BlockOverlayEvent extends RenderBlockScreenEffectEvent {
        public static final CancellableEventBus<BlockOverlayEvent> BUS = CancellableEventBus.create(BlockOverlayEvent.class);

        @ApiStatus.Internal
        public BlockOverlayEvent(Player player, BlockState blockState, BlockPos blockPos) {
            super(player, blockState, blockPos);
        }
    }

    /**
     * Fired when the player is under normal Water.
     */
    public static final class WaterOverlayEvent extends RenderBlockScreenEffectEvent {
        public static final CancellableEventBus<WaterOverlayEvent> BUS = CancellableEventBus.create(WaterOverlayEvent.class);

        private final PlayerRenderState renderState;

        @ApiStatus.Internal
        public WaterOverlayEvent(Player player, BlockState blockState, BlockPos blockPos, PlayerRenderState renderState) {
            super(player, blockState, blockPos);
            this.renderState = renderState;
        }

        /** The current computed render state, waterOverlay is accessible. */
        public PlayerRenderState getRenderState() {
            return this.renderState;
        }
    }
}
