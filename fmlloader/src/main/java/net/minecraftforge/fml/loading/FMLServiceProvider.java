/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.loading;

import com.mojang.logging.LogUtils;
import cpw.mods.modlauncher.api.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static net.minecraftforge.fml.loading.LogMarkers.CORE;

public class FMLServiceProvider implements ITransformationService {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String name() {
        return "fml";
    }

    @Override
    public void initialize(IEnvironment environment) {
    }

    @Override
    public List<Resource> beginScanning(final IEnvironment environment) {
        return List.of(FMLLoader.getPluginResources());
    }

    @Override
    public List<Resource> completeScan(final IModuleLayerManager layerManager) {
        return List.of(FMLLoader.completeScan(layerManager));
    }

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) throws IncompatibleEnvironmentException {
         if (ModDiscoveryService.onLoadException != null)
             throw ModDiscoveryService.onLoadException;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NotNull List<ITransformer> transformers() {
        LOGGER.debug(CORE, "Loading coremod transformers");
        return new ArrayList<>(FMLLoader.getCoreModProvider().getCoreModTransformers());
    }
}
