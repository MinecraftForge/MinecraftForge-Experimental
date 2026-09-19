/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryValidator;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public sealed interface DataPackRegistryEvent {
    /**
     * Fired when datapack registries can be registered.
     * Datapack registries are registries which can only load entries through JSON files from datapacks.
     * <p>
     * Data JSONs will be loaded from {@code data/<datapack_namespace>/modid/registryname/}, where {@code modid} is the namespace of the registry key.
     * <p>
     * This event is fired on both {@linkplain LogicalSide logical sides}.
     */
    final class NewRegistry extends MutableEvent implements DataPackRegistryEvent {
        public static final EventBus<NewRegistry> BUS = EventBus.create(NewRegistry.class);

        private final List<Builder<?>> builders = new ArrayList<>();

        @ApiStatus.Internal
        public NewRegistry() {}

        /**
         * Registers the given registry key as an unsynced datapack registry, which will cause data to be loaded from
         * a datapack folder based on the registry's name. The datapack registry is not required to be present
         * on clients when connecting to servers with the mod/registry.
         * <p>
         * Data JSONs will be loaded from {@code data/<datapack_namespace>/modid/registryname/}, where {@code modid} is the namespace of the registry key.
         *
         * @param registryKey the root registry key of the new datapack registry
         * @param codec the codec to be used for loading data from datapacks on servers
         * @see #dataPackRegistry(ResourceKey, Codec, Codec)
         */
        public <T> void dataPackRegistry(ResourceKey<Registry<T>> registryKey, Codec<T> codec) {
            builder(registryKey, codec);
        }

        /**
         * Registers the registry key as a datapack registry, which will cause data to be loaded from
         * a datapack folder based on the registry's name.
         * <p>
         * Data JSONs will be loaded from {@code data/<datapack_namespace>/modid/registryname/}, where {@code modid} is the namespace of the registry key.
         *
         * @param registryKey the root registry key of the new datapack registry
         * @param codec the codec to be used for loading data from datapacks on servers
         * @param networkCodec the codec to be used for syncing loaded data to clients.
         * If {@code networkCodec} is null, data will not be synced, and clients are not required to have this
         * datapack registry to join a server.
         * <p>
         * If {@code networkCodec} is not null, clients must have this datapack registry/mod
         * when joining a server that has this datapack registry/mod.
         * The data will be synced using the network codec and accessible via {@link ClientPacketListener#registryAccess()}.
         * @see #dataPackRegistry(ResourceKey, Codec)
         */
        public <T> void dataPackRegistry(ResourceKey<? extends Registry<T>> registryKey, Codec<T> codec, @Nullable Codec<T> networkCodec) {
            builder(registryKey, codec).sync(networkCodec);
        }

        public <T> Builder<T> builder(ResourceKey<? extends Registry<T>> registryKey, Codec<T> codec) {
            var ret = new Builder<T>(registryKey, codec);
            builders.add(ret);
            return ret;
        }

        public static class Builder<T> {
            private final ResourceKey<? extends Registry<T>> key;
            private final Codec<T> codec;
            private @Nullable Codec<T> networkCodec;
            private boolean reloadable = false;
            private RegistryValidator<T> validator = RegistryValidator.none();

            private Builder(ResourceKey<? extends Registry<T>> key, final Codec<T> codec) {
                this.key = key;
                this.codec = codec;
            }

            public Builder<T> sync(Codec<T> codec) {
                if (this.networkCodec != null)
                    throw new IllegalArgumentException("Network codec is already set");
                this.networkCodec = codec;
                return this;
            }

            public Builder<T> reloadable() {
                this.reloadable = true;
                return this;
            }

            public Builder<T> world() {
                this.reloadable = false;
                return this;
            }

            public Builder<T> validator(RegistryValidator<T> validator) {
                this.validator = validator;
                return this;
            }
        }

        void process() {
            for (var builder : this.builders) {
                @SuppressWarnings("unchecked")
                var typed = (Builder<Object>)builder;
                DataPackRegistriesHooks.addRegistryCodec(typed.key, typed.codec, typed.networkCodec, typed.reloadable, typed.validator);
            }
        }
    }
}
