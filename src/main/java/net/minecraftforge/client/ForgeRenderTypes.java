/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.TextureFormat;

import net.minecraft.util.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.common.util.NonNullSupplier;
import net.minecraftforge.fml.earlydisplay.DisplayWindow;

import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus;

public enum ForgeRenderTypes {
    ITEM_LAYERED_SOLID(()-> getItemLayeredSolid(blockAtlas())),
    ITEM_LAYERED_CUTOUT(()-> getItemLayeredCutout(blockAtlas())),
    ITEM_LAYERED_CUTOUT_MIPPED(()-> getItemLayeredCutoutMipped(blockAtlas())),
    ITEM_LAYERED_TRANSLUCENT(()-> getItemLayeredTranslucent(blockAtlas())),
    ITEM_UNSORTED_TRANSLUCENT(()-> getUnsortedTranslucent(blockAtlas())),
    ITEM_UNLIT_TRANSLUCENT(()-> getUnlitTranslucent(blockAtlas())),
    ITEM_UNSORTED_UNLIT_TRANSLUCENT(()-> getUnlitTranslucent(blockAtlas(), false));

    /**
     * Controls the texture filtering state for certain {@link RenderType Render Types}.
     *
     * @see ForgeRenderTypes#getText
     * @see ForgeRenderTypes#getTextIntensity
     * @see ForgeRenderTypes#getTextPolygonOffset
     * @see ForgeRenderTypes#getTextIntensityPolygonOffset
     * @see ForgeRenderTypes#getTextSeeThrough
     * @see ForgeRenderTypes#getTextIntensitySeeThrough
     */
    public static boolean enableTextTextureLinearFiltering = false;

    @SuppressWarnings("deprecation")
    private static Identifier blockAtlas() {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    /**
     * @return A RenderType fit for multi-layer solid item rendering.
     */
    public static RenderType getItemLayeredSolid(Identifier textureLocation) {
        return Internal.LAYERED_ITEM_SOLID.apply(textureLocation);
    }

    /**
     * @return A RenderType fit for multi-layer cutout item item rendering.
     */
    public static RenderType getItemLayeredCutout(Identifier textureLocation) {
        return Internal.LAYERED_ITEM_CUTOUT.apply(textureLocation);
    }

    /**
     * @return A RenderType fit for multi-layer cutout-mipped item rendering.
     */
    public static RenderType getItemLayeredCutoutMipped(Identifier textureLocation) {
        return Internal.LAYERED_ITEM_CUTOUT_MIPPED.apply(textureLocation);
    }

    /**
     * @return A RenderType fit for multi-layer translucent item rendering.
     */
    public static RenderType getItemLayeredTranslucent(Identifier textureLocation) {
        return Internal.LAYERED_ITEM_TRANSLUCENT.apply(textureLocation);
    }

    /**
     * @return A RenderType fit for translucent item/entity rendering, but with depth sorting disabled.
     */
    public static RenderType getUnsortedTranslucent(Identifier textureLocation) {
        return Internal.UNSORTED_TRANSLUCENT.apply(textureLocation);
    }

    /**
     * @return A RenderType fit for translucent item/entity rendering, but with diffuse lighting disabled
     * so that fullbright quads look correct.
     */
    public static RenderType getUnlitTranslucent(Identifier textureLocation) {
        return Internal.UNLIT_TRANSLUCENT_SORTED.apply(textureLocation);
    }

    /**
     * @return A RenderType fit for translucent item/entity rendering, but with diffuse lighting disabled
     * so that fullbright quads look correct.
     * @param sortingEnabled If false, depth sorting will not be performed.
     */
    public static RenderType getUnlitTranslucent(Identifier textureLocation, boolean sortingEnabled) {
        return (sortingEnabled ? Internal.UNLIT_TRANSLUCENT_SORTED : Internal.UNLIT_TRANSLUCENT_UNSORTED).apply(textureLocation);
    }

    /**
     * @return Same as {@link RenderType#entityCutout(Identifier)}, but with mipmapping enabled.
     */
    public static RenderType getEntityCutoutMipped(Identifier textureLocation) {
        return Internal.LAYERED_ITEM_CUTOUT_MIPPED.apply(textureLocation);
    }

    /**
     * @see #enableTextTextureLinearFiltering
     *
     * @return Replacement of {@link RenderType#text(Identifier)}, but with optional linear texture filtering.
     */
    public static RenderType getText(Identifier locationIn) {
        return Internal.TEXT.apply(locationIn);
    }

    /**
     * @see #enableTextTextureLinearFiltering
     *
     * @return Replacement of {@link RenderType#textIntensity(Identifier)}, but with optional linear texture filtering.
     */
    public static RenderType getTextIntensity(Identifier locationIn) {
        return Internal.TEXT_INTENSITY.apply(locationIn);
    }

    /**
     * @see #enableTextTextureLinearFiltering
     *
     * @return Replacement of {@link RenderType#textPolygonOffset(Identifier)}, but with optional linear texture filtering.
     */
    public static RenderType getTextPolygonOffset(Identifier locationIn) {
        return Internal.TEXT_POLYGON_OFFSET.apply(locationIn);
    }

    /**
     * @see #enableTextTextureLinearFiltering
     *
     * @return Replacement of {@link RenderType#textIntensityPolygonOffset(Identifier)}, but with optional linear texture filtering.
     */
    public static RenderType getTextIntensityPolygonOffset(Identifier locationIn) {
        return Internal.TEXT_INTENSITY_POLYGON_OFFSET.apply(locationIn);
    }

    /**
     * @see #enableTextTextureLinearFiltering
     *
     * @return Replacement of {@link RenderType#textSeeThrough(Identifier)}, but with optional linear texture filtering.
     */
    public static RenderType getTextSeeThrough(Identifier locationIn) {
        return Internal.TEXT_SEE_THROUGH.apply(locationIn);
    }

    /**
     * @see #enableTextTextureLinearFiltering
     *
     * @return Replacement of {@link RenderType#textIntensitySeeThrough(Identifier)}, but with optional linear texture filtering.
     */
    public static RenderType getTextIntensitySeeThrough(Identifier locationIn) {
        return Internal.TEXT_INTENSITY_SEE_THROUGH.apply(locationIn);
    }

    /**
     * Render type for rendering the early loading screen, not for modder consumption.
     *
     * @param window The early loading screen, so we can grab the buffer ID
     */
    @ApiStatus.Internal
    public static RenderType getLoadingOverlay(DisplayWindow window) {
        return Internal.getLoadingOverlay(window);
    }

    // ----------------------------------------
    //  Implementation details below this line
    // ----------------------------------------

    private final NonNullSupplier<RenderType> renderTypeSupplier;

    ForgeRenderTypes(NonNullSupplier<RenderType> renderTypeSupplier) {
        // Wrap in a Lazy<> to avoid running the supplier more than once.
        this.renderTypeSupplier = NonNullLazy.of(renderTypeSupplier);
    }

    public RenderType get() {
        return renderTypeSupplier.get();
    }

    // TODO: [VEN] Note that the names of these render types have been modified to match the names of their container constants,
    //             This was done for the sake of consistency, but it may be wrong. Need validation.
    //             Additionally, I am unsure if I have set all the flags correctly. Most of these don't have full docs on
    //             what they are meant to be used for, so I had to guess how to remap them to the new system.
    private static abstract class Internal {
        public static Function<Identifier, RenderType> UNSORTED_TRANSLUCENT = Util.memoize(Internal::unsortedTranslucent);
        private static RenderType unsortedTranslucent(Identifier texture) {
            return RenderType.create("forge_unsorted_translucent",
                RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .withTexture("Sampler0", texture)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .affectsCrumbling()
                    .useLightmap()
                    .useOverlay()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
            );
        }

        public static Function<Identifier, RenderType> UNLIT_TRANSLUCENT_SORTED = Util.memoize(Internal::unlitTranslucentSorted);
        private static RenderType unlitTranslucentSorted(Identifier texture) {
            return RenderType.create("forge_unlit_translucent_sorted",
                RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .affectsCrumbling()
                    .sortOnUpload()
                    .withTexture("Sampler0", texture)
                    .useOverlay()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
            );
        }

        public static Function<Identifier, RenderType> UNLIT_TRANSLUCENT_UNSORTED = Util.memoize(Internal::unlitTranslucentUnsorted);
        private static RenderType unlitTranslucentUnsorted(Identifier texture) {
            return RenderType.create("forge_unlit_translucent_sorted",
                RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .affectsCrumbling()
                    .withTexture("Sampler0", texture)
                    .useOverlay()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
            );
        }

        public static Function<Identifier, RenderType> LAYERED_ITEM_SOLID = Util.memoize(Internal::layeredItemSolid);
        private static RenderType layeredItemSolid(Identifier texture) {
            return RenderType.create("forge_layered_item_soild",
                RenderSetup.builder(RenderPipelines.ENTITY_SOLID)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .affectsCrumbling()
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
            );
        }

        public static Function<Identifier, RenderType> LAYERED_ITEM_CUTOUT = Util.memoize(Internal::layeredItemCutout);
        private static RenderType layeredItemCutout(Identifier texture) {
            return RenderType.create("forge_layered_item_cutout",
                RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .affectsCrumbling()
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
            );
        }

        public static Function<Identifier, RenderType> LAYERED_ITEM_CUTOUT_MIPPED = Util.memoize(Internal::layeredItemCutoutMipped);
        private static RenderType layeredItemCutoutMipped(Identifier texture) {
            return RenderType.create("forge_layered_item_cutout_mipped",
                RenderSetup.builder(RenderPipelines.ENTITY_SMOOTH_CUTOUT)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .affectsCrumbling()
                    .withTexture("Sampler0", texture) // Used to have mipmap = true
                    .useLightmap()
                    .useOverlay()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
            );
        }

        public static Function<Identifier, RenderType> LAYERED_ITEM_TRANSLUCENT = Util.memoize(Internal::layeredItemTranslucent);
        private static RenderType layeredItemTranslucent(Identifier texture) {
            return RenderType.create("forge_layered_item_translucent",
                RenderSetup.builder(RenderPipelines.ITEM_ENTITY_TRANSLUCENT_CULL)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .affectsCrumbling()
                    .sortOnUpload()
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()
            );
        }

        public static Function<Identifier, RenderType> TEXT = Util.memoize(Internal::getText);
        private static RenderType getText(Identifier texture) {
            return RenderType.create(
                "forge_text",
                RenderSetup.builder(RenderPipelines.TEXT)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                    .sortOnUpload() // This is what is different from RenderTypes.TEXT
                    .createRenderSetup()
            );
        }

        public static Function<Identifier, RenderType> TEXT_INTENSITY = Util.memoize(Internal::getTextIntensity);
        private static RenderType getTextIntensity(Identifier texture) {
            return RenderType.create("forge_text_intensity",
                RenderSetup.builder(RenderPipelines.TEXT_INTENSITY)
                    .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup()
            );
        }

        public static Function<Identifier, RenderType> TEXT_POLYGON_OFFSET = Util.memoize(Internal::getTextPolygonOffset);
        private static RenderType getTextPolygonOffset(Identifier texture) {
            return RenderType.create("forge_text_polygon_offset",
                RenderSetup.builder(RenderPipelines.TEXT_POLYGON_OFFSET)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .sortOnUpload()
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .createRenderSetup()
            );
        }

        public static Function<Identifier, RenderType> TEXT_INTENSITY_POLYGON_OFFSET = Util.memoize(Internal::getTextIntensityPolygonOffset);
        private static RenderType getTextIntensityPolygonOffset(Identifier texture) {
            return RenderType.create("forge_text_intensity_polygon_offset",
                RenderSetup.builder(RenderPipelines.TEXT_INTENSITY)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .sortOnUpload()
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .createRenderSetup()
            );
        }

        public static Function<Identifier, RenderType> TEXT_SEE_THROUGH = Util.memoize(Internal::getTextSeeThrough);
        private static RenderType getTextSeeThrough(Identifier texture) {
            return RenderType.create("forge_text_see_through",
                RenderSetup.builder(RenderPipelines.TEXT_SEE_THROUGH)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .createRenderSetup()
            );
        }

        public static Function<Identifier, RenderType> TEXT_INTENSITY_SEE_THROUGH = Util.memoize(Internal::getTextIntensitySeeThrough);
        private static RenderType getTextIntensitySeeThrough(Identifier texture) {
            return RenderType.create("forge_text_intensity_see_through",
                RenderSetup.builder(RenderPipelines.TEXT_INTENSITY_SEE_THROUGH)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .sortOnUpload()
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .createRenderSetup()
            );
        }

        private static final RenderPipeline LOADING_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation("pipeline/forge/loading_overlay")
                .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .build();

        private static final GpuSampler LOADING_SAMPLER = RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.NEAREST, false);
        private static final Identifier LOADING_TEXTURE = Identifier.fromNamespaceAndPath("forge", "loading_overlay");

        public static RenderType getLoadingOverlay(DisplayWindow window) {
            var gpu = RenderSystem.getDevice();
            var texture = gpu.createTexture(LOADING_TEXTURE.toString(), 5, TextureFormat.RGBA8,
                    window.context().width(), window.context().height(),
                    1, window.getFramebufferTextureId());
            var textureView = gpu.createTextureView(texture);

            return RenderType.create("forge_loading_overlay",
                RenderSetup.builder(LOADING_PIPELINE)
                    .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                    .withTexture("Sampler0", textureView, LOADING_SAMPLER)
                    .createRenderSetup()
            );
        }
    }
}
