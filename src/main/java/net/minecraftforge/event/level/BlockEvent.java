/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.level;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.HasResult;
import net.minecraftforge.common.util.Result;

import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

public sealed interface BlockEvent
        permits BlockEvent.BreakEvent, BlockEvent.CropGrowEvent,
        BlockEvent.EntityPlaceEvent, BlockEvent.FarmlandTrampleEvent, BlockEvent.FluidPlaceBlockEvent,
        BlockEvent.NeighborNotifyEvent, BlockEvent.PortalSpawnEvent, NoteBlockEvent, PistonEvent {
    boolean DEBUG = Boolean.parseBoolean(System.getProperty("forge.debugBlockEvent", "false"));

    LevelAccessor getLevel();

    BlockPos getPos();

    BlockState getState();

    /// Event that is fired when an Block is about to be broken by a player.
    ///
    /// Setting the result to [Result#DENY] will prevent the Block from being broken.
    final class BreakEvent extends MutableEvent implements Cancellable, BlockEvent, HasResult {
        public static final CancellableEventBus<BreakEvent> BUS = CancellableEventBus.create(BreakEvent.class);

        private final LevelAccessor level;
        private final BlockPos pos;
        private final BlockState state;

        /// Reference to the Player who broke the block
        private final Player player;
        private int exp;
        private Result result;

        public BreakEvent(Level level, BlockPos pos, BlockState state, Player player, Result result) {
            this.level = level;
            this.pos = pos;
            this.state = state;
            this.player = player;
            this.result = result;

            if (state == null || !ForgeHooks.isCorrectToolForDrops(state, player)) { // Handle empty block or player unable to break block scenario
                this.exp = 0;
            } else {
                var lookup = level.registryAccess().lookup(Registries.ENCHANTMENT).get();

                int fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(lookup.getOrThrow(Enchantments.FORTUNE), player.getMainHandItem());
                int silkTouchLevel = EnchantmentHelper.getItemEnchantmentLevel(lookup.getOrThrow(Enchantments.SILK_TOUCH), player.getMainHandItem());
                this.exp = state.getExpDrop(level, level.getRandom(), pos, fortuneLevel, silkTouchLevel);
            }
        }

        @Override
        public LevelAccessor getLevel() {
            return level;
        }

        @Override
        public BlockPos getPos() {
            return pos;
        }

        @Override
        public BlockState getState() {
            return state;
        }

        public Player getPlayer() {
            return player;
        }

        /// Get the experience dropped by the block after the event has processed
        ///
        /// @return The experience to drop or 0 if the event was denied
        public int getExpToDrop() {
            return this.getResult().isDenied() ? 0 : exp;
        }

        /// Set the amount of experience dropped by the block after the event has processed
        ///
        /// @param exp 1 or higher to drop experience, else nothing will drop
        public void setExpToDrop(int exp) {
            this.exp = exp;
        }

        @Override
        public Result getResult() {
            return this.result;
        }

        @Override
        public void setResult(Result result) {
            this.result = result;
        }
    }

    /// Called when a block is placed.
    ///
    /// If a Block Place event is cancelled, the block will not be placed.
    sealed class EntityPlaceEvent extends MutableEvent implements Cancellable, BlockEvent {
        public static final CancellableEventBus<EntityPlaceEvent> BUS = CancellableEventBus.create(EntityPlaceEvent.class);

        private final LevelAccessor level;
        private final BlockPos pos;

        private final Entity entity;
        private final BlockSnapshot blockSnapshot;
        private final BlockState placedBlock;
        private final BlockState placedAgainst;

        public EntityPlaceEvent(@NotNull BlockSnapshot blockSnapshot, @NotNull BlockState placedAgainst, @Nullable Entity entity) {
            this.level = blockSnapshot.getLevel();
            this.pos = blockSnapshot.getPos();
            this.entity = entity;
            this.blockSnapshot = blockSnapshot;
            this.placedBlock = !(entity instanceof Player) ? blockSnapshot.getReplacedBlock() : blockSnapshot.getCurrentBlock();
            this.placedAgainst = placedAgainst;

            if (DEBUG) {
                System.out.printf("Created EntityPlaceEvent - [PlacedBlock: %s ][PlacedAgainst: %s ][Entity: %s ]\n", getPlacedBlock(), placedAgainst, entity);
            }
        }

        @Override
        public LevelAccessor getLevel() {
            return level;
        }

        @Override
        public BlockPos getPos() {
            return pos;
        }

        @Override
        public BlockState getState() {
            return placedBlock;
        }

        @Nullable
        public Entity getEntity() { return entity; }
        public BlockSnapshot getBlockSnapshot() { return blockSnapshot; }
        public BlockState getPlacedBlock() { return placedBlock; }
        public BlockState getPlacedAgainst() { return placedAgainst; }
    }

    /**
     * Fired when a single block placement triggers the
     * creation of multiple blocks(e.g. placing a bed block). The block returned
     * by {@link #state} and its related methods is the block where
     * the placed block would exist if the placement only affected a single
     * block.
     */
    final class EntityMultiPlaceEvent extends EntityPlaceEvent implements Cancellable {
        public static final CancellableEventBus<EntityMultiPlaceEvent> BUS = CancellableEventBus.create(EntityMultiPlaceEvent.class);

        private final List<BlockSnapshot> blockSnapshots;

        public EntityMultiPlaceEvent(@NotNull List<BlockSnapshot> blockSnapshots, @NotNull BlockState placedAgainst, @Nullable Entity entity) {
            super(blockSnapshots.getFirst(), placedAgainst, entity);
            this.blockSnapshots = List.copyOf(blockSnapshots);
            if (DEBUG) {
                System.out.printf("Created EntityMultiPlaceEvent - [PlacedAgainst: %s ][Entity: %s ]\n", placedAgainst, entity);
            }
        }

        /**
         * Gets a list of BlockSnapshots for all blocks which were replaced by the
         * placement of the new blocks. Most of these blocks will just be of type AIR.
         *
         * @return immutable list of replaced BlockSnapshots
         */
        public List<BlockSnapshot> getReplacedBlockSnapshots() {
            return blockSnapshots;
        }
    }

    /**
     * Fired when a physics update occurs on a block. This event acts as
     * a way for mods to detect physics updates, in the same way a BUD switch
     * does. This event is only called on the server.
     *
     * @param getNotifiedSides list of directions from the base block that updates will occur upon
     * @param getForceRedstoneUpdate if the flag was set during setBlock call (0x16 to flags)
     */
    record NeighborNotifyEvent(
            LevelAccessor getLevel,
            BlockPos getPos,
            BlockState getState,
            EnumSet<Direction> getNotifiedSides,
            boolean getForceRedstoneUpdate
    ) implements Cancellable, BlockEvent, RecordEvent {
        public static final CancellableEventBus<NeighborNotifyEvent> BUS = CancellableEventBus.create(NeighborNotifyEvent.class);
    }

    /**
     * Fired to check whether a non-source block can turn into a source block.
     * A result of ALLOW causes a source block to be created even if the liquid
     * usually doesn't do that (like lava), and a result of DENY prevents creation
     * even if the liquid usually does do that (like water).
     */
    record CreateFluidSourceEvent(Level getLevel, BlockPos getPos, BlockState getState, Result.Holder resultHolder)
            implements RecordEvent, HasResult.Record {
        public static final EventBus<CreateFluidSourceEvent> BUS = EventBus.create(CreateFluidSourceEvent.class);

        public CreateFluidSourceEvent(Level level, BlockPos pos, BlockState state) {
            this(level, pos, state, new Result.Holder());
        }
    }

    /// Fired when a liquid places a block. Use [#setNewState(BlockState)] to change the result of
    /// a cobblestone generator or add variants of obsidian. Alternatively, you  could execute
    /// arbitrary code when lava sets blocks on fire, even preventing it.
    final class FluidPlaceBlockEvent extends MutableEvent implements Cancellable, BlockEvent {
        public static final CancellableEventBus<FluidPlaceBlockEvent> BUS = CancellableEventBus.create(FluidPlaceBlockEvent.class);

        private final LevelAccessor level;
        private final BlockPos pos;
        private final BlockState state;

        private final BlockPos liquidPos;
        private BlockState newState;
        private final BlockState origState;

        public FluidPlaceBlockEvent(LevelAccessor level, BlockPos pos, BlockPos liquidPos, BlockState state) {
            this.level = level;
            this.pos = pos;
            this.state = state;
            this.liquidPos = liquidPos;
            this.newState = state;
            this.origState = level.getBlockState(pos);
        }

        @Override
        public LevelAccessor getLevel() {
            return level;
        }

        /// @return The position of the block to be changed.
        @Override
        public BlockPos getPos() {
            return pos;
        }

        /// @return The block that was originally going to be placed.
        @Override
        public BlockState getState() {
            return state;
        }

        /// @return The position of the liquid this event originated from. This may be the same as [#getPos()].
        public BlockPos getLiquidPos() {
            return liquidPos;
        }

        /// @return The block state that will be placed after this event resolves.
        public BlockState getNewState() {
            return newState;
        }

        public void setNewState(BlockState state) {
            this.newState = state;
        }

        /// @return The state of the block to be changed before the event was fired.
        public BlockState getOriginalState() {
            return origState;
        }
    }

    /// Fired when a crop block grows. See subevents.
    sealed interface CropGrowEvent extends BlockEvent, InheritableEvent {
        EventBus<CropGrowEvent> BUS = EventBus.create(CropGrowEvent.class);

        /// Fired when any "growing age" blocks (for example cacti, chorus plants, or crops
        /// in vanilla) attempt to advance to the next growth age state during a random tick.
        ///
        /// - [Result#DEFAULT] will pass on to the vanilla growth mechanics.
        /// - [Result#ALLOW] will force the plant to advance a growth stage.
        /// - [Result#DENY] will prevent the plant from advancing a growth stage.
        record Pre(LevelAccessor getLevel, BlockPos getPos, BlockState getState, Result.Holder resultHolder)
                implements CropGrowEvent, HasResult.Record {
            public static final EventBus<Pre> BUS = EventBus.create(Pre.class);

            public Pre(Level level, BlockPos pos, BlockState state) {
                this(level, pos, state, new Result.Holder());
            }
        }

        /// Fired when "growing age" blocks (for example cacti, chorus plants, or crops in vanilla) have successfully
        /// grown. The block's original state is available, in addition to its new state.
        record Post(LevelAccessor getLevel, BlockPos getPos, BlockState getState, BlockState getOriginalState)
                implements CropGrowEvent {
            public static final EventBus<Post> BUS = EventBus.create(Post.class);
        }
    }

    /// Fired when farmland gets trampled by an entity and is about to turn into dirt.
    ///
    /// This event is [Cancellable]. Cancelling prevents the farmland from turning into dirt.
    ///
    /// @param getLevel The level the farmland block is in
    /// @param getPos The position of the farmland block in the level
    /// @param getFallDistance The distance the entity fell before landing on the farmland block
    /// @param getEntity The entity that trampled the farmland block
    @NullMarked
    record FarmlandTrampleEvent(
            LevelAccessor getLevel,
            BlockPos getPos,
            BlockState getState,
            double getFallDistance,
            Entity getEntity
    ) implements Cancellable, BlockEvent, RecordEvent {
        public static final CancellableEventBus<FarmlandTrampleEvent> BUS = CancellableEventBus.create(FarmlandTrampleEvent.class);
    }

    /// Fired when an attempt is made to spawn a nether portal from
    /// [BaseFireBlock#onPlace(BlockState, Level, BlockPos, BlockState, boolean)].
    ///
    /// This event is [Cancellable]. If cancelled, the portal will not be spawned.
    @NullMarked
    record PortalSpawnEvent(LevelAccessor getLevel, BlockPos getPos, BlockState getState, PortalShape getPortalSize)
            implements Cancellable, BlockEvent, RecordEvent {
        public static final CancellableEventBus<PortalSpawnEvent> BUS = CancellableEventBus.create(PortalSpawnEvent.class);
    }
}
