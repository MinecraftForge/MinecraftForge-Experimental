/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.extensions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraftforge.client.model.IQuadTransformer;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

/**
 * Extension interface for {@link VertexConsumer}.
 */
public interface IForgeVertexConsumer {
    private VertexConsumer self() {
        return (VertexConsumer)this;
    }

    /**
     * Consumes an unknown {@link VertexFormatElement} as a raw int data array.
     * <p>
     * If the consumer needs to store the data for later use, it must copy it. There are no guarantees on immutability.
     */
    default VertexConsumer misc(VertexFormatElement element, int... rawData) {
        return self();
    }

    /**
     * Variant with no per-vertex shading.
     */
    default void putBulkData(PoseStack.Pose pose, BakedQuad bakedQuad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay, boolean readExistingColor) {
        self().putBulkData(pose, bakedQuad, new float[] { 1.0F, 1.0F, 1.0F, 1.0F }, red, green, blue, alpha, new int[] { packedLight, packedLight, packedLight, packedLight }, packedOverlay, readExistingColor);
    }

    default int applyBakedLighting(int packedLight, BakedQuad data, int vertex) {
        return packedLight;
        /*
        long existing = data.packedUV(vertex);
        float blBaked = UVPair.unpackU(existing);
        float slBaked = UVPair.unpackV(existing);
        int bl = Math.max(LightTexture.block(packedLight), blBaked);
        int sl = Math.max(LightTexture.sky(packedLight), slBaked);
        return LightTexture.pack(bl, sl);
        */
    }

    default void applyBakedNormals(Vector3f generated, BakedQuad data, int vertex, Matrix3f normalTransform) {
        /*
        byte nx = data.get(28);
        byte ny = data.get(29);
        byte nz = data.get(30);
        if (nx != 0 || ny != 0 || nz != 0) {
            generated.set(nx / 127f, ny / 127f, nz / 127f);
            generated.mul(normalTransform);
        }
        */
    }
}
