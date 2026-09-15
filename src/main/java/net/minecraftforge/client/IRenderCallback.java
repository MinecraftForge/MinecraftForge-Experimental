/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * Simple callback for the 'submit' stage of the renderer.
 * Used for fluid/fire overlays
 */
@FunctionalInterface
public interface IRenderCallback {
    void render(PoseStack pose, SubmitNodeCollector collector);
}
