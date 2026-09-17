/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.network.packets;

import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraftforge.registries.DataPackRegistriesHooks;
import net.minecraftforge.registries.RegistryManager;

public record RegistryList(
    int token,
    List<Identifier> normal,
    List<ResourceKey<? extends Registry<?>>> datapacks) {

    public static final StreamCodec<FriendlyByteBuf, RegistryList> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RegistryList::token,
        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), RegistryList::normal,
        ResourceKey.REGISTRY_STREAM_CODEC.apply(ByteBufCodecs.list()), RegistryList::datapacks,
        RegistryList::new
    );

    public RegistryList(int token) {
        this(token, RegistryManager.getRegistryNamesForSyncToClient(), List.copyOf(DataPackRegistriesHooks.getSyncedCustomRegistries()));
    }
}