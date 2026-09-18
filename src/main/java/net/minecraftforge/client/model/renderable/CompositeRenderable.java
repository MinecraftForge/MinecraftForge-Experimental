/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model.renderable;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * A renderable object composed of a hierarchy of parts, each made up of a number of meshes.
 * <p>
 * Each mesh renders a set of quads using a different texture.
 *
 * @see Builder
 */
public class CompositeRenderable implements IRenderable<CompositeRenderable.Transforms> {
    private final List<Component> components = new ArrayList<>();

    private CompositeRenderable() { }

    @Override
    public void render(PoseStack poseStack, ITextureRenderTypeLookup textureRenderTypeLookup, int lightmap, int overlay, float partialTick, Transforms context) {
        for (var component : components)
            component.render(poseStack, textureRenderTypeLookup, lightmap, overlay, context);
    }

    public static Builder builder() {
        return new Builder();
    }

    private record Component(String name, List<Component> children, List<Mesh> meshes) {
        private Component(String name) {
            this(name, new ArrayList<>(), new ArrayList<>());
        }

        public void render(PoseStack poseStack, ITextureRenderTypeLookup textureRenderTypeLookup, int lightmap, int overlay, Transforms context) {
            Matrix4f matrix = context.getTransform(name);
            if (matrix != null) {
                poseStack.pushPose();
                poseStack.mulPose(matrix);
            }

            for (var part : children)
                part.render(poseStack, textureRenderTypeLookup, lightmap, overlay, context);

            for (var mesh : meshes)
                mesh.render(poseStack, textureRenderTypeLookup, lightmap, overlay);

            if (matrix != null)
                poseStack.popPose();
        }
    }

    private record Mesh(List<BakedQuad> quads, QuadInstance quadInstance) {
//        private final Identifier texture;

        private Mesh(Identifier texture) {
//            this.texture = texture;
            this(new ArrayList<>(), new QuadInstance());
        }

        public void render(PoseStack poseStack, ITextureRenderTypeLookup textureRenderTypeLookup, int lightmap, int overlay) {
            //var consumer = bufferSource.getBuffer(textureRenderTypeLookup.get(texture));
            quadInstance.setLightCoords(lightmap);
            quadInstance.setOverlayCoords(overlay);

            //for (var quad : quads)
            //    consumer.putBakedQuad(poseStack.last(), quad, quadInstance);
        }
    }

    public record Builder(CompositeRenderable get) {
        private Builder() {
            this(new CompositeRenderable());
        }

        public PartBuilder<Builder> child(String name) {
            var child = new Component(name);
            get.components.add(child);
            return new PartBuilder<>(this, child);
        }
    }

    public static class PartBuilder<T> {
        private final T parent;
        private final Component component;

        private PartBuilder(T parent, Component component) {
            this.parent = parent;
            this.component = component;
        }

        public PartBuilder<PartBuilder<T>> child(String name) {
            var child = new Component(component.name + "/" + name);
            this.component.children.add(child);
            return new PartBuilder<>(this, child);
        }

        public PartBuilder<T> addMesh(Identifier texture, List<BakedQuad> quads) {
            var mesh = new Mesh(texture);
            mesh.quads.addAll(quads);
            component.meshes.add(mesh);
            return this;
        }

        public T end() {
            return parent;
        }
    }

    /**
     * A context value that provides {@link Matrix4f} transforms for certain parts of the model.
     */
    public static class Transforms {
        /**
         * A default instance that has no transforms specified.
         */
        public static final Transforms EMPTY = new Transforms(ImmutableMap.of());

        /**
         * Builds a MultipartTransforms object with the given mapping.
         */
        public static Transforms of(ImmutableMap<String, Matrix4f> parts) {
            return new Transforms(parts);
        }

        private final ImmutableMap<String, Matrix4f> parts;

        private Transforms(ImmutableMap<String, Matrix4f> parts) {
            this.parts = parts;
        }

        @Nullable
        public Matrix4f getTransform(String part) {
            return parts.get(part);
        }
    }
}
