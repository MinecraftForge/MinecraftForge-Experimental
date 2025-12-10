/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import java.util.List;

/**
 * Transformer for {@link BakedQuad baked quads}.
 *
 * @see QuadTransformers
 */
@FunctionalInterface
public interface IQuadTransformer {
    BakedQuad process(BakedQuad quad);

    default List<BakedQuad> process(List<BakedQuad> inputs) {
        return inputs.stream().map(this::process).toList();
    }

    default IQuadTransformer andThen(IQuadTransformer other) {
        return quad -> other.process(process(quad));
    }
}
