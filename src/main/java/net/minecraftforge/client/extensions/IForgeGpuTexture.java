/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.extensions;

public interface IForgeGpuTexture {
    default boolean isStencilEnabled() {
        return false;
    }
}
