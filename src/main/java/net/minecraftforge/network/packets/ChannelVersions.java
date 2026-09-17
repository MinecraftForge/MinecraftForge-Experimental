/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.network.packets;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraftforge.network.NetworkRegistry;

public record ChannelVersions(Map<Identifier, @NotNull Integer> channels) {
    public static StreamCodec<FriendlyByteBuf, ChannelVersions> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(Object2IntOpenHashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT),
        ChannelVersions::channels,
        ChannelVersions::new
    );

    public ChannelVersions() {
        this(NetworkRegistry.buildChannelVersions());
    }
}