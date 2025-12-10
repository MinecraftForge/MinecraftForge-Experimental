/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model.geometry;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.mojang.math.Transformation;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.ModelDiscovery.ModelWrapper;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.model.ForgeBlockModelData;

@ApiStatus.Internal
public record ModelContext(
    ResolvedModel self,
    ResolvedModel parent,
    @Nullable ForgeBlockModelData data,
    boolean gui3d
) implements IGeometryBakingContext {
    public ModelContext(ResolvedModel self) {
        this(self, self.parent(), makeData(self, self.parent()), true);
    }

    private static ForgeBlockModelData makeData(ResolvedModel self, ResolvedModel parent) {
        var data = self.wrapped() instanceof BlockModel block ? block.forgeData() : null;

        var p = parent;
        while (p != null && (data == null || !data.full())) {
            if (p.wrapped() instanceof BlockModel block) {
                if (data == null)
                    data = block.forgeData();
                else
                    data = data.merge(block.forgeData());
            }
            p = (ModelWrapper)p.parent();
        }

        return data;
    }

    @Override
    public boolean isGui3d() {
        return gui3d;
    }

    public ModelContext withGui3d(boolean value) {
        return new ModelContext(self, parent, data, value);
    }

    @Override
    public boolean useBlockLight() {
        var value = self.wrapped().guiLight();
        return value != null && value.lightLikeBlock();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return Boolean.TRUE.equals(self.wrapped().ambientOcclusion());
    }

    @Override
    public ItemTransforms getTransforms() {
        return self.wrapped().transforms();
    }

    @Override
    public Transformation getRootTransform() {
        return data == null ? Transformation.identity() : data.transform().orElse(Transformation.identity());
    }

    @Override
    public @Nullable Identifier getRenderTypeHint() {
        return data == null ? null : data.renderType().orElse(null);
    }

    @Override
    public boolean isComponentVisible(String component, boolean fallback) {
        if (data == null || data.visibility().isEmpty())
            return fallback;
        return data.visibility().get().getOrDefault(component, fallback);
    }
}
