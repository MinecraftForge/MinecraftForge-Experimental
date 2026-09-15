/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.extensions;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraftforge.registries.DeferredRegisterData;

public interface IForgeRegistrySetBuilder {
    private RegistrySetBuilder self() {
        return (RegistrySetBuilder)this;
    }

    default <T> RegistrySetBuilder add(DeferredRegisterData<T> dr) {
        return self().add(dr.getRegistryKey(), dr);
    }
}
