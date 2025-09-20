/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.client.DeltaTracker;
import net.minecraft.server.MinecraftServer;

import java.util.function.BooleanSupplier;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.fml.LogicalSide;

public sealed abstract class TickEvent extends MutableEvent {
    public final LogicalSide side;

    protected TickEvent(LogicalSide side) {
        this.side = side;
    }

    public static sealed abstract class ServerTickEvent extends TickEvent {
        private final BooleanSupplier haveTime;
        private final MinecraftServer server;

        protected ServerTickEvent(BooleanSupplier haveTime, MinecraftServer server) {
            super(LogicalSide.SERVER);
            this.haveTime = haveTime;
            this.server = server;
        }

        /**
         * @return {@code true} whether the server has enough time to perform any
         * additional tasks (usually IO related) during the current tick,
         * otherwise {@code false}
         */
        public boolean haveTime() {
            return this.haveTime.getAsBoolean();
        }

        /**
         * {@return the server instance}
         */
        public MinecraftServer getServer() {
            return server;
        }

        public static final class Pre extends ServerTickEvent {
            public static final EventBus<Pre> BUS = EventBus.create(Pre.class);

            public Pre(BooleanSupplier haveTime, MinecraftServer server) {
                super(haveTime, server);
            }
        }

        public static final class Post extends ServerTickEvent {
            public static final EventBus<Post> BUS = EventBus.create(Post.class);

            public Post(BooleanSupplier haveTime, MinecraftServer server) {
                super(haveTime, server);
            }
        }
    }

    public static sealed abstract class ClientTickEvent extends TickEvent {
        protected ClientTickEvent() {
            super(LogicalSide.CLIENT);
        }

        public static final class Pre extends ClientTickEvent {
            public static final EventBus<Pre> BUS = EventBus.create(Pre.class);
            public static final Pre INSTANCE = new Pre();
        }

        public static final class Post extends ClientTickEvent {
            public static final EventBus<Post> BUS = EventBus.create(Post.class);
            public static final Post INSTANCE = new Post();
        }
    }

    public static sealed abstract class LevelTickEvent extends TickEvent {
        public final Level level;
        private final BooleanSupplier haveTime;

        protected LevelTickEvent(LogicalSide side, Level level, BooleanSupplier haveTime) {
            super(side);
            this.level = level;
            this.haveTime = haveTime;
        }

        /**
         * @return {@code true} whether the server has enough time to perform any
         * additional tasks (usually IO related) during the current tick,
         * otherwise {@code false}
         * @see ServerTickEvent#haveTime()
         */
        public boolean haveTime() {
            return this.haveTime.getAsBoolean();
        }

        public static final class Pre extends LevelTickEvent {
            public static final EventBus<Pre> BUS = EventBus.create(Pre.class);

            public Pre(LogicalSide side, Level level, BooleanSupplier haveTime) {
                super(side, level, haveTime);
            }
        }

        public static final class Post extends LevelTickEvent {
            public static final EventBus<Post> BUS = EventBus.create(Post.class);

            public Post(LogicalSide side, Level level, BooleanSupplier haveTime) {
                super(side, level, haveTime);
            }
        }
    }

    public static sealed abstract class PlayerTickEvent extends TickEvent {
        public final Player player;

        protected PlayerTickEvent(Player player) {
            super(player instanceof ServerPlayer ? LogicalSide.SERVER : LogicalSide.CLIENT);
            this.player = player;
        }

        public static final class Pre extends PlayerTickEvent {
            public static final EventBus<Pre> BUS = EventBus.create(Pre.class);

            public Pre(Player player) {
                super(player);
            }
        }

        public static final class Post extends PlayerTickEvent {
            public static final EventBus<Post> BUS = EventBus.create(Post.class);

            public Post(Player player) {
                super(player);
            }
        }
    }

    public static sealed abstract class RenderTickEvent extends TickEvent {
        private final DeltaTracker timer;

        private RenderTickEvent(DeltaTracker timer) {
            super(LogicalSide.CLIENT);
            this.timer = timer;
        }

        public DeltaTracker getTimer() {
            return this.timer;
        }

        public static final class Pre extends RenderTickEvent {
            public static final EventBus<Pre> BUS = EventBus.create(Pre.class);

            public Pre(DeltaTracker timer) {
                super(timer);
            }
        }

        public static final class Post extends RenderTickEvent {
            public static final EventBus<Post> BUS = EventBus.create(Post.class);

            public Post(DeltaTracker timer) {
                super(timer);
            }
        }

    }
}
