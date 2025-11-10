/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.common.asm;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;

import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraftforge.unsafe.UnsafeHacks;

public class MixinBootstrapper implements ITransformationService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker MARKER = MarkerManager.getMarker("MIXIN");

    public MixinBootstrapper() {
        bootstrap();
    }

    @Override
    public @NotNull String name() {
        return "MixinBootstrapper";
    }

    @Override
    public void initialize(IEnvironment environment) {
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NotNull List<ITransformer> transformers() {
        return List.of();
    }

    @Override
    public List<Resource> completeScan(IModuleLayerManager layerManager) {
        return List.of();
    }

    private void bootstrap() {
        var layerManager = Launcher.INSTANCE.findLayerManager().orElseThrow(() -> new IllegalStateException("Could not find Layer Manager"));
        var layer = layerManager.getLayer(Layer.SERVICE).orElseThrow(() -> new IllegalStateException("Could not find SERVICE layer in completeScan"));
        var module = layer.findModule("org.spongepowered.mixin").orElse(null);

        // If mixin wasn't loaded we don't need to do any work.
        if (module == null)
            return;

        // Mixin's onLoad calls context classloader. Which is currently BOOT, lets set it to Mixin's.
        Thread.currentThread().setContextClassLoader(module.getClassLoader());

        // Mixin-Extras needs access to internal stuff in Mixin.
        // SecureJar just makes everything open unless told otherwise
        // If we make it respect the module info we would need to add opens, but we don't
        //openAllPackages(module);

        var fileName = fileName(module);

        var services = new HashMap<String, List<String>>();
        for (var provider : module.getDescriptor().provides())
            services.put(provider.service(), provider.providers());

        for (var provider : services.getOrDefault(ILaunchPluginService.class.getName(), List.of())) {
            try {
                @SuppressWarnings("unchecked")
                var cls = (Class<ILaunchPluginService>)Class.forName(provider, true, module.getClassLoader());

                /**
                 * Nothing is called in a ILaunchPluginService before we hook in.
                 * So we do not need to do anything more then just register it.
                 */
                try {
                    var srvc = (ILaunchPluginService)cls.getConstructor().newInstance();
                    LazyPlugins.plugins.put(srvc.name(), srvc);
                    LazyModList.add(srvc.name(), "PLUGINSERVICE", fileName);
                } catch (Throwable e) {
                    LOGGER.fatal(MARKER, "Encountered serious error loading launch plugin service. Things will not work well", e);
                }
            } catch (Throwable e) {
                sneak(e);
            }
        }
    }

    /*
    private static void openAllPackages(Module module) {
        for (var pkg : module.getDescriptor().packages())
            addOpens(module, pkg);
    }

    private static Method implAddOpens;
    private static void addOpens(Module target, String pkg) {
        try {
            if (implAddOpens == null) {
                implAddOpens = Module.class.getDeclaredMethod("implAddOpens", String.class);
                UnsafeHacks.setAccessible(implAddOpens);
            }
            implAddOpens.invoke(target, pkg);
        } catch (Throwable t) {
            sneak(t);
        }
    }
    */

    @SuppressWarnings("unchecked")
    public static <R, E extends Throwable> R sneak(Throwable e) throws E {
        throw (E) e;
    }

    private static String fileName(Module module) {
        var ref = module.getLayer().configuration().findModule(module.getName());
        if (ref.isEmpty())
            return "MISSING FILE";
        var location = ref.get().reference().location();
        if (location.isEmpty())
            return "MISSING FILE";

        var uri = location.get();
        var str = uri.toString();
        if (str.startsWith("jar:")) {
            str = str.substring(4, str.length() - 2);
            uri = URI.create(str);
        }

        return Path.of(uri).toString();
    }

    private static class LazyPlugins {
        private static final Map<String, ILaunchPluginService> plugins = get();

        private static Map<String, ILaunchPluginService> get() {
            var launchPlugins = UnsafeHacks.<Launcher, LaunchPluginHandler>findField(Launcher.class, "launchPlugins").get(Launcher.INSTANCE);
            return UnsafeHacks.<LaunchPluginHandler, Map<String, ILaunchPluginService>>findField(LaunchPluginHandler.class, "plugins").get(launchPlugins);
        }
    }

    private static class LazyModList {
        private static final List<Map<String, String>> modlist = getModList();

        private static List<Map<String, String>> getModList() {
            return Launcher.INSTANCE.environment()
                .getProperty(IEnvironment.Keys.MODLIST.get())
                .orElseThrow(() -> new IllegalStateException("Invalid environment, Missing MODLIST"));
        }

        private static void add(String name, String type, String file) {
            modlist.add(Map.of(
                "name", name,
                "type", type,
                "file", file
            ));
        }
    }
}
