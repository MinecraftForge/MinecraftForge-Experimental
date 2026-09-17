/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.network.packets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraftforge.network.NetworkContext.NetworkMismatchData;
import net.minecraftforge.network.NetworkContext.NetworkMismatchData.Version;

/**
 * Notifies the client of a channel mismatch on the server, so a {@link net.minecraftforge.client.gui.ModMismatchDisconnectedScreen} is used to notify the user of the disconnection.
 * This packet also sends the data of a channel mismatch (currently, the ids and versions of the mismatched channels) to the client for it to display the correct information in said screen.
 */
public record MismatchData(
    Map<Identifier, Version> mismatched,
    Set<Identifier> missing
) {
    private static final int MAX_LENGTH = 0x100;
    private static final StreamCodec<ByteBuf, String> STRING_CODEC = ByteBufCodecs.stringUtf8(MAX_LENGTH);
    private static final StreamCodec<ByteBuf, Identifier> IDENNTIFIER_CODEC = STRING_CODEC.map(Identifier::parse, Identifier::toString);
    public static final StreamCodec<FriendlyByteBuf, MismatchData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new,
            IDENNTIFIER_CODEC,
            StreamCodec.composite(STRING_CODEC, Version::received, STRING_CODEC, Version::had, Version::new)
        ),
        MismatchData::mismatched,
        ByteBufCodecs.collection(HashSet::new, IDENNTIFIER_CODEC),
        MismatchData::missing,
        MismatchData::new
    );

    public MismatchData(NetworkMismatchData data) {
        this(data.mismatched(), data.missing());
    }
}