/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.capabilities;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.bus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class CapabilityProvider<B extends ICapabilityProviderImpl<B>> implements ICapabilityProviderImpl<B> {

    public interface CapabilitySupplier<B> {
        AttachCapabilitiesEvent<B> create(B provider);
    }

    @VisibleForTesting
    static boolean SUPPORTS_LAZY_CAPABILITIES = true;

    private @Nullable CapabilityDispatcher capabilities;
    private boolean valid = true;

    private boolean                       isLazy             = false;
    private Supplier<ICapabilityProvider> lazyParentSupplier = null;
    private CompoundTag                   lazyData           = null;
    private HolderLookup.Provider         registryAccess     = null;
    private boolean initialized = false;

    protected CapabilityProvider() {
        this(false);
    }

    protected CapabilityProvider(final boolean isLazy) {
        this.isLazy = SUPPORTS_LAZY_CAPABILITIES && isLazy;
    }

    protected final void gatherCapabilities() {
        gatherCapabilities(() -> null);
    }

    protected final void gatherCapabilities(@Nullable ICapabilityProvider parent) {
        gatherCapabilities(() -> parent);
    }

    protected final void gatherCapabilities(@Nullable Supplier<ICapabilityProvider> parent) {
        if (isLazy && !initialized) {
            lazyParentSupplier = parent == null ? () -> null : parent;
            return;
        }

        doGatherCapabilities(parent == null ? null : parent.get());
    }

    private void doGatherCapabilities(@Nullable ICapabilityProvider parent) {
        var event = postEvent(getProvider());
        this.capabilities = !event.getCapabilities().isEmpty() || parent != null ? new CapabilityDispatcher(event.getCapabilities(), event.getListeners(), parent) : null;
        this.initialized = true;
    }

    protected abstract AttachCapabilitiesEvent<?> postEvent(B provider);

    @SuppressWarnings("unchecked")
    @NotNull
    B getProvider() {
        return (B)this;
    }

    protected final @Nullable CapabilityDispatcher getCapabilities() {
        if (isLazy && !initialized) {
            doGatherCapabilities(lazyParentSupplier == null ? null : lazyParentSupplier.get());
            if (lazyData != null)
                deserializeCaps(registryAccess, lazyData);
        }

        return capabilities;
    }

    protected final @Nullable CompoundTag serializeCaps(HolderLookup.Provider registryAccess) {
        if (isLazy && !initialized)
            return lazyData;

        var disp = getCapabilities();
        if (disp != null)
            return disp.serializeNBT(registryAccess);

        return null;
    }

    protected final void deserializeCaps(HolderLookup.Provider registryAccess, CompoundTag tag) {
        if (isLazy && !initialized) {
            this.lazyData = tag;
            this.registryAccess = registryAccess;
            return;
        }

        var disp = getCapabilities();
        if (disp != null)
            disp.deserializeNBT(registryAccess, tag);

        this.lazyData = null;
        this.registryAccess = null;
    }

    /*
     * Invalidates all the contained caps, and prevents getCapability from returning a value.
     * This is usually called when the object in question is removed from the world.
     * However there may be cases where modders want to copy these 'invalid' caps.
     * They should call reviveCaps while they are doing their work, and then call invalidateCaps again
     * when they are finished.
     * Be sure to make your invalidate callbaks recursion safe.
     */
    public void invalidateCaps() {
        this.valid = false;
        final CapabilityDispatcher disp = getCapabilities();
        if (disp != null)
            disp.invalidate();
    }

    /*
     * This function will allow getCability to return values again.
     * Modders can use this if they need to copy caps from one removed provider to a new one.
     * It is expected the modders who call this function, then call invalidateCaps() to invalidate the provider again.
     */
    public void reviveCaps() {
        this.valid = true; //Stupid players don't copy the entity when transporting across worlds.
    }

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        final CapabilityDispatcher disp = getCapabilities();
        return !valid || disp == null ? LazyOptional.empty() : disp.getCapability(cap, side);
    }

    /**
     * Special implementation for cases which have a superclass and can't extend CapabilityProvider directly.
     * See {@link LevelChunk}
     */
    public static class AsField<B extends ICapabilityProviderImpl<B>> extends CapabilityProvider<B> {
        private final B owner;
        private final CapabilitySupplier<B> supplier;

        public AsField(final CapabilitySupplier<B> eventSupplier, B owner) {
            this.supplier = eventSupplier;
            this.owner = owner;
        }

        public AsField(final CapabilitySupplier<B> eventSupplier, B owner, boolean isLazy) {
            super(isLazy);
            this.supplier = eventSupplier;
            this.owner = owner;
        }

        public void initInternal() {
            gatherCapabilities();
        }

        @Nullable
        public CompoundTag serializeInternal(HolderLookup.Provider registryAccess) {
            return serializeCaps(registryAccess);
        }

        public void deserializeInternal(HolderLookup.Provider registryAccess, CompoundTag tag) {
            deserializeCaps(registryAccess, tag);
        }

        @Override
        protected AttachCapabilitiesEvent<?> postEvent(B provider) {
            return supplier.create(provider);
        }

        @Override
        @NotNull
        B getProvider() {
            return owner;
        }
    };

}