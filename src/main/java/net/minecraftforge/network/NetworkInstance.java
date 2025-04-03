/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.network;

import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.network.Channel.VersionTest;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus;

import io.netty.util.AttributeKey;

/**
 * This is essentially the shared common class for {@link SimpleChannel} and {@link EventNetworkChannel}.
 * I've now introduced {@link Channel} as that common modder facing base class. I am basically using this
 * as the internal API and {@link Channel} as the public.
 */
@ApiStatus.Internal
public record NetworkInstance(
        BusGroup networkEventBusGroup,
        EventBus<CustomPayloadEvent> eventBus,
        ResourceLocation channelName,
        int networkProtocolVersion,
        VersionTest clientAcceptedVersions,
        VersionTest serverAcceptedVersions,
        Map<AttributeKey<?>, Function<Connection, ?>> attributes,
        Consumer<Connection> channelHandler,
        ServerStatusPing.ChannelData pingData,
        Set<ResourceLocation> ids
) {
    // We use an event bus here so that we don't have to have a handle(event) public function on Channel.
    // Should this be changed so that modders can fire other channel's handlers?
    // Todo: [Forge][Networking] Update the above comment

    static NetworkInstance of(ResourceLocation channelName, int networkProtocolVersion,
        VersionTest clientAcceptedVersions, VersionTest serverAcceptedVersions,
        Map<AttributeKey<?>, Function<Connection, ?>> attributes, Consumer<Connection> channelHandler
    ) {
        return new NetworkInstance(
                channelName, networkProtocolVersion, clientAcceptedVersions, serverAcceptedVersions,
                BusGroup.create(channelName.toString() + "_networkInstance", CustomPayloadEvent.class),
                attributes, channelHandler);
    }

    public NetworkInstance(ResourceLocation channelName, int networkProtocolVersion,
        VersionTest clientAcceptedVersions, VersionTest serverAcceptedVersions, BusGroup busGroup,
        Map<AttributeKey<?>, Function<Connection, ?>> attributes, Consumer<Connection> channelHandler
    ) {
        this(busGroup, EventBus.create(busGroup, CustomPayloadEvent.class), channelName, networkProtocolVersion,
                clientAcceptedVersions, serverAcceptedVersions, attributes, channelHandler,
                new ServerStatusPing.ChannelData(channelName, networkProtocolVersion, clientAcceptedVersions.accepts(VersionTest.Status.MISSING, -1)),
                new HashSet<>());
    }

    public void addListener(Consumer<CustomPayloadEvent> eventListener) {
        // TODO: [Forge][Networking] Adjust this to use the new event bus system
        eventBus.addListener(eventListener);
    }

    public void registerObject(final Object object) {
        // TODO: [Forge][Networking] Adjust this to use the new event bus system
//        this.networkEventBusGroup.register(object);
    }

    public void unregisterObject(final Object object) {
        // TODO: [Forge][Networking] Adjust this to use the new event bus system
//        this.networkEventBusGroup.unregister(object);
    }

    public boolean dispatch(CustomPayloadEvent event) {
        this.eventBus.post(event);
        return event.getSource().getPacketHandled();
    }

    /**
     * Registers another name that will have its CustomPayloadEvents redirected to this channel.
     * Like the main name, this must be unique across all channels.
     */
    public NetworkInstance addChild(ResourceLocation name) {
        NetworkRegistry.register(this, name);
        this.ids.add(name);
        return this;
    }

    ResourceLocation getChannelName() {
        return channelName;
    }

    int getNetworkProtocolVersion() {
        return networkProtocolVersion;
    }

    void registrationChange(ResourceLocation name, boolean registered) {
        // TODO: Expose to listeners?
    }

    boolean isRemotePresent(Connection con) {
        var channels = NetworkContext.get(con).getRemoteChannels();
        return channels.containsAll(ids);
    }

    public BusGroup getNetworkEventBusGroup() {
        return networkEventBusGroup;
    }
}
