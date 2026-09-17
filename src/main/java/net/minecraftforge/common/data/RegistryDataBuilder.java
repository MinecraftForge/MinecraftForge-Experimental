/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.RegistrySetBuilder.PatchedRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.data.registries.RegistryPatchGenerator;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * An extension of the {@link RegistriesDatapackGenerator} which properly handles
 * referencing existing dynamic registry objects within another dynamic registry
 * object.
 */
public class RegistryDataBuilder {
    public static RegistryDataBuilder of() {
        return new RegistryDataBuilder();
    }

    private static final CompletableFuture<HolderLookup.Provider> VANILLA_WORLD = CompletableFuture.supplyAsync(VanillaRegistries::createWorldLookup, Util.backgroundExecutor());
    private static final CompletableFuture<HolderLookup.Provider> VANILLA_RELOADABLE = VANILLA_WORLD.thenApplyAsync(VanillaRegistries::createReloadableLookup, Util.backgroundExecutor());

    public static CompletableFuture<HolderLookup.Provider> vanillaWorld() {
        return VANILLA_WORLD;
    }
    public static CompletableFuture<HolderLookup.Provider> vanillaReloadable() {
        return VANILLA_RELOADABLE;
    }

    private static RegistrySetBuilder fill(Consumer<RegistrySetBuilder> filler) {
        var ret = new RegistrySetBuilder();
        filler.accept(ret);
        return ret;
    }

    private CompletableFuture<PatchedRegistries> world = null;
    private CompletableFuture<HolderLookup.Provider> worldFull = VANILLA_WORLD;

    private CompletableFuture<PatchedRegistries> reload = null;
    private CompletableFuture<HolderLookup.Provider> reloadFull = VANILLA_RELOADABLE;

    private Set<String> modIds;
    private Predicate<ResourceKey<?>> filter;

    private RegistryDataBuilder() {
        resetFilter();
    }

    private @Nullable String name;
    /** Sets the name for the DataGenerator, will be suffixed by "world" or "reloadable" */
    public RegistryDataBuilder name(String value) {
        this.name = value;
        return this;
    }

    /** Adds a modid to the output filter. If the name is unset, will set it to the mod id */
    public RegistryDataBuilder modid(String value) {
        this.modIds.add(value);
        if (this.name == null)
            name(value);
        return this;
    }

    /** Resets the modid list, and output filter to default values */
    public RegistryDataBuilder resetFilter() {
        this.modIds = new HashSet<>();
        this.filter = key -> modIds.isEmpty() || modIds.contains(key.identifier().getNamespace());
        return this;
    }

    /** Sets the entry filter that will be used in the data generators, */
    public RegistryDataBuilder filter(Predicate<ResourceKey<?>> value) {
        this.filter = value;
        return this;
    }

    /** Adds a new layer, which is populated by the provided consumers */
    public RegistryDataBuilder layer(Consumer<RegistrySetBuilder> world, Consumer<RegistrySetBuilder> reloadable) {
        layer(fill(world), fill(reloadable));
        return this;
    }

    /** Adds a new layer using pre-built Registry Sets */
    public RegistryDataBuilder layer(RegistrySetBuilder world, RegistrySetBuilder reloadable) {
        return world(world).reloadable(reloadable);
    }

    /** Adds a new world layer populated by the provided consumer */
    public RegistryDataBuilder world(Consumer<RegistrySetBuilder> filler) {
        return world(fill(filler));
    }

    /** Adds a new world layer using a pre-built Registry Set */
    public RegistryDataBuilder world(RegistrySetBuilder layer) {
        world = RegistryPatchGenerator.createWorldLookup(worldFull, layer);
        worldFull = world.thenApply(PatchedRegistries::full);
        return this;
    }

    /** Returns the patch subset for the current world layer. Throws IllegalStateException if called with out calling {@link #world(RegistrySetBulder)} */
    public CompletableFuture<HolderLookup.Provider> world() {
        if (world == null)
            throw new IllegalStateException("Attempted to get world layer patches without defining a world layer");
        return world.thenApply(PatchedRegistries::patches);
    }

    /** Returns the full registry context for the current world layer. Useful for passing to other data providers */
    public CompletableFuture<HolderLookup.Provider> worldFull() {
        return worldFull;
    }

    /** Creates a DataGenerator for the patches of the current world layer. Throws IllegalStateException if called with out calling {@link #world(RegistrySetBulder)} */
    public RegistriesDatapackGenerator worldGenerator(final PackOutput output) {
        return new RegistriesDatapackGenerator(output, name == null ? "world" : name + " world", RegistryDataLoader.getWorldAndDimensionRegistries(), world(), filter);
    }

    /** Adds a new reloadable layer populated by the provided consumer */
    public RegistryDataBuilder reloadable(Consumer<RegistrySetBuilder> filler) {
        return reloadable(fill(filler));
    }

    /** Adds a new reloadable layer using a pre-built Registry Set */
    public RegistryDataBuilder reloadable(RegistrySetBuilder layer) {
        reload = RegistryPatchGenerator.createReloadableLookup(worldFull, reloadFull, layer);
        reloadFull = world.thenApply(PatchedRegistries::full);
        return this;
    }

    /** Returns the patch subset for the current reloadable layer. Throws IllegalStateException if called with out calling {@link #reloadable(RegistrySetBulder)} */
    public CompletableFuture<HolderLookup.Provider> reloadable() {
        if (reload == null)
            throw new IllegalStateException("Attempted to get reloadable layer patches without defining a reloadable layer");
        return reload.thenApply(PatchedRegistries::patches);
    }

    /** Returns the full registry context for the current reloadable layer. Useful for passing to other data providers */
    public CompletableFuture<HolderLookup.Provider> reloadableFull() {
        return reloadFull;
    }

    /** Creates a DataGenerator for the patches of the current world layer. Throws IllegalStateException if called with out calling {@link #reloadable(RegistrySetBulder)} */
    public RegistriesDatapackGenerator reloadableGenerator(final PackOutput output) {
        return new RegistriesDatapackGenerator(output, name == null ? "reloadable" : name + " reloadable", RegistryDataLoader.RELOADABLE_REGISTRIES, reloadable(), filter);
    }
}
