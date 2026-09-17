/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.extensions;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.registries.DeferredRegisterData;

public interface IForgeRegistrySetBuilder {
    private RegistrySetBuilder self() {
        return (RegistrySetBuilder)this;
    }

    default <T> RegistrySetBuilder add(DeferredRegisterData<T> dr) {
        return self().add(dr.getRegistryKey(), dr);
    }

    default RegistrySetBuilder copy() {
        return new RegistrySetBuilder().add(self());
    }

    default HolderLookup.Provider buildBuiltIn() {
        return self().build(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }
}
