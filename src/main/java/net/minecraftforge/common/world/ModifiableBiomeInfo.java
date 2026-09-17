/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.world;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.ClimateSettings;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;

/**
 * Holds lazy-evaluable modified biome info.
 * Memoizers are not used because it's important to return null
 * without evaluating the biome info if it's accessed outside of a server context.
 */
public class ModifiableBiomeInfo {
    @NotNull
    private final BiomeInfo originalBiomeInfo;
    @NotNull
    private final Consumer<BiomeInfo> callback;
    @Nullable
    private BiomeInfo modifiedBiomeInfo = null;

    /**
     * @param originalBiomeInfo BiomeInfo representing the original state of a biome when the biome was constructed.
     */
    @ApiStatus.Internal
    public ModifiableBiomeInfo(@NotNull final BiomeInfo originalBiomeInfo, @NotNull final Consumer<BiomeInfo> callback) {
        this.originalBiomeInfo = originalBiomeInfo;
        this.callback = callback;
    }

    /**
     * {@return The modified biome info if modified biome info has been generated, otherwise gets original biome info}
     */
    @NotNull
    public BiomeInfo get() {
        return this.modifiedBiomeInfo == null
            ? originalBiomeInfo
            : modifiedBiomeInfo;
    }

    /**
     * {@return The original biome info that the associated biome was created with}
     */
    @NotNull
    public BiomeInfo getOriginalBiomeInfo() {
        return this.originalBiomeInfo;
    }

    /**
     * {@return Modified biome info; null if it hasn't been set yet}
     */
    @Nullable
    public BiomeInfo getModifiedBiomeInfo() {
        return this.modifiedBiomeInfo;
    }

    /**
     * Internal forge method; the game will crash if mods invoke this.
     * Creates and caches the modified biome info.
     * @param biome named biome with original data.
     * @param biomeModifiers biome modifiers to apply.
     *
     * @throws IllegalStateException if invoked more than once.
     */
    @ApiStatus.Internal
    public void applyBiomeModifiers(final Holder<Biome> biome, final List<BiomeModifier> biomeModifiers) {
        if (this.modifiedBiomeInfo != null)
            throw new IllegalStateException(String.format(Locale.ENGLISH, "Biome %s already modified", biome));

        var original = this.getOriginalBiomeInfo();
        var builder = original.builder();
        for (var phase : BiomeModifier.Phase.values()) {
            for (var modifier : biomeModifiers)
                modifier.modify(biome, phase, builder);
        }
        this.modifiedBiomeInfo = builder.build();
        this.callback.accept(this.modifiedBiomeInfo);
    }

    /**
     * Record containing raw biome data.
     * @param climateSettings Weather and temperature settings.
     * @param effects Client-relevant effects for rendering and sound.
     * @param generationSettings Worldgen features and carvers.
     * @param mobSpawnSettings Mob spawn settings.
     */
    public record BiomeInfo(
        ClimateSettings climateSettings,
        EnvironmentAttributeMap attributes,
        BiomeSpecialEffects effects,
        BiomeGenerationSettings generationSettings
    ) {
        /**
         * @param original the biome to copy
         * @return A ModifiedBiomeInfo.Builder with a copy of the biome's data
         */
        private Builder builder() {
            var climateBuilder = ClimateSettingsBuilder.copyOf(climateSettings());
            var attributes = EnvironmentAttributeMap.builder().putAll(attributes());
            var effectsBuilder = BiomeSpecialEffectsBuilder.copyOf(effects());
            var generationBuilder = new BiomeGenerationSettings.PlainBuilder();
            generationBuilder.addFrom(generationSettings());

            return new Builder(
                climateBuilder,
                attributes,
                effectsBuilder,
                generationBuilder
            );
        }



        public record Builder(
            ClimateSettingsBuilder climateSettings,
            EnvironmentAttributeMap.Builder attributes,
            BiomeSpecialEffectsBuilder effects,
            BiomeGenerationSettings.PlainBuilder generationSettings
        ) {
            public BiomeInfo build() {
                return new BiomeInfo(
                    this.climateSettings.build(),
                    this.attributes.build(),
                    this.effects.build(),
                    this.generationSettings.build()
                );
            }
        }
    }
}
