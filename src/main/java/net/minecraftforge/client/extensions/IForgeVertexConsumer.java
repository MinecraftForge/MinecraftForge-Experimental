/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.extensions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;

/**
 * Extension interface for {@link VertexConsumer}.
 */
public interface IForgeVertexConsumer {
    private VertexConsumer self() {
        return (VertexConsumer)this;
    }

    /**
     * Variant with no per-vertex shading.
     */
    default void putBulkData(PoseStack.Pose pose, BakedQuad bakedQuad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay) {
        self().putBulkData(pose, bakedQuad, new float[] { 1.0F, 1.0F, 1.0F, 1.0F }, red, green, blue, alpha, new int[] { packedLight, packedLight, packedLight, packedLight }, packedOverlay);
    }
}
