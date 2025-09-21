/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.server;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;

public sealed interface ServerLifecycleEvent extends InheritableEvent
        permits ServerAboutToStartEvent, ServerStartedEvent, ServerStartingEvent, ServerStoppedEvent, ServerStoppingEvent {
    EventBus<ServerLifecycleEvent> BUS = EventBus.create(ServerLifecycleEvent.class);

    MinecraftServer getServer();
}
