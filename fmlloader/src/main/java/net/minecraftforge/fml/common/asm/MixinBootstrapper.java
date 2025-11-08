/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.common.asm;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;

import cpw.mods.modlauncher.ArgumentHandler;
import cpw.mods.modlauncher.Environment;
import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformationServiceDecorator;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraftforge.unsafe.UnsafeFieldAccess;
import net.minecraftforge.unsafe.UnsafeHacks;

public class MixinBootstrapper implements ITransformationService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker MARKER = MarkerManager.getMarker("MIXIN");
    private IEnvironment environment;

    @Override
    public @NotNull String name() {
        return "MixinBootstrapper";
    }

    @Override
    public void initialize(IEnvironment environment) {
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
        this.environment = env;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NotNull List<ITransformer> transformers() {
        return List.of();
    }

    @Override
    public List<Resource> completeScan(IModuleLayerManager layerManager) {
        var layer = layerManager.getLayer(Layer.PLUGIN).orElseThrow(() -> new IllegalStateException("Could not find PLUGIN layer in completeScan"));
        var module = layer.findModule("org.spongepowered.mixin").orElse(null);

        // If mixin wasn't loaded we don't need to do any work.
        if (module == null)
            return List.of();

        // Mixin-Extras needs access to internal stuff in Mixin. I have no idea why this worked before. But lets just open Mixin to everyone
        openAllPackages(module);

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

        var itransformers = services.getOrDefault(ITransformationService.class.getName(), List.of());
        if (itransformers.isEmpty())
            return List.of();

        // To prevent concurrent modification exceptions, we need to set the service field to a new map as we're in its iterator right now.
        var oldServiceLookup = LazyTransforms.services.get(LazyTransforms.handler);
        var newServiceLookup = new HashMap<>(oldServiceLookup);
        LazyTransforms.services.set(LazyTransforms.handler, newServiceLookup);

        record Transformer(ITransformationService service, String name, TransformationServiceDecorator wrapper) {}
        var transformers = new ArrayList<Transformer>();
        for (var provider : itransformers) {
            try {
                @SuppressWarnings("unchecked")
                var cls = (Class<ITransformationService>)Class.forName(provider, true, module.getClassLoader());
                var service = wrap(() -> (ITransformationService)cls.getConstructor().newInstance()).get();

                // Create Service and add to mod list
                LazyModList.add(service.name(), "TRANSFORMATIONSERVICE", fileName);

                // Create wrapper and add to service handler
                var wrapper = LazyTransforms.wrapper.apply(service);
                newServiceLookup.put(service.name(), wrapper);
                transformers.add(new Transformer(service, provider, wrapper));
            } catch (Throwable e) {
                sneak(e);
            }
        }

        /**
         * Several things have already been called for Transformation services.
         * Basically we need to mimic TransformationServicesHandler.initializeTransformationServices
         *
         * And then call completeScan
         */
        if (transformers.isEmpty())
            return List.of();

        var oldCl = Thread.currentThread().getContextClassLoader();
        try {
            // OnLoad calls context classloader. So lets set it to one that knows about Mixin.
            Thread.currentThread().setContextClassLoader(module.getClassLoader());
            for (var t : transformers) {
                LOGGER.debug(MARKER, "Loading Mixin Transformation Service {}", t.name);
                LazyTransforms.onLoad.call(t.wrapper, environment, newServiceLookup.keySet());

                if (!LazyTransforms.isValid.test(t.wrapper)) {
                    LOGGER.error(MARKER, "Mixin Transformation service {} failed to load", t.name);
                    throw new IllegalStateException("Invalid Services found " + t.name);
                }
            }

            // call process arguments so that it can find dev time values
            // Note: this calls it on EVERY service because it needs to read the whole argument list
            // I'd have to do some hacks with jopt if I wanted to get around that.
            LazyTransforms.processArguments.get();

            for (var t : transformers) {
                LOGGER.debug(MARKER, "Initalizing Mixin Transformation Service {}", t.name);
                LazyTransforms.onInitalize.accept(t.wrapper);
            }

            // This is a noop on mixin currently, and we're too late for the values to be used because this is to find the PLUGIN layer.
            // So technically we don't need to call it
            /*
            for (var t : transformers) {
                LOGGER.debug(MARKER, "Running Mixin Transformation Service scan {}", t.name);
                LazyTransforms.runScan.accept(t.wrapper);
            }
            */

            // Last but not least, we need to call complete scan, and return whatever it wants us to.
            var ret = new ArrayList<Resource>();
            for (var t : transformers) {
                LOGGER.debug(MARKER, "Completing Mixin Transformation Service scan {}", t.name);
                var resources = LazyTransforms.onCompleteScan.call(t.wrapper, layerManager);
                ret.addAll(resources);
            }
            return ret;
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

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

    @SuppressWarnings("unchecked")
    public static <R, E extends Throwable> R sneak(Throwable e) throws E {
        throw (E) e;
    }

    private static <T> Supplier<T> wrap(Callable<T> func) {
        return () -> {
            try {
                return func.call();
            } catch (Throwable e) {
                return sneak(e);
            }
        };
    }

    private static String fileName(Module module) {
        var ref = module.getLayer().configuration().findModule(module.getName());
        if (ref.isEmpty())
            return "MISSING FILE";
        var location = ref.get().reference().location();
        if (location.isEmpty())
            return "MISSING FILE";

        var path = Path.of(location.get());
        return path.toString();
    }

    private static class LazyPlugins {
        private static final Map<String, ILaunchPluginService> plugins = get();

        private static Map<String, ILaunchPluginService> get() {
            var launchPlugins = UnsafeHacks.<Launcher, LaunchPluginHandler>findField(Launcher.class, "launchPlugins").get(Launcher.INSTANCE);
            return UnsafeHacks.<LaunchPluginHandler, Map<String, ILaunchPluginService>>findField(LaunchPluginHandler.class, "plugins").get(launchPlugins);
        }
    }

    private static class LazyTransforms {
        private static final Object handler = UnsafeHacks.<Launcher, Object>findField(Launcher.class, "transformationServicesHandler").get(Launcher.INSTANCE);
        private static final ArgumentHandler arguments = UnsafeHacks.<Launcher, ArgumentHandler>findField(Launcher.class, "argumentHandler").get(Launcher.INSTANCE);
        @SuppressWarnings({ "unchecked", "rawtypes" })
        private static final UnsafeFieldAccess<Object, Map<String, TransformationServiceDecorator>> services = UnsafeHacks.<Object, Map<String, TransformationServiceDecorator>>findField((Class)handler.getClass(), "serviceLookup");
        private static final NewWrapper wrapper = getWrapper();
        private static final OnLoad onLoad = getOnLoad();
        private static final Predicate<TransformationServiceDecorator> isValid = getIsValid();
        private static final Supplier<Object> processArguments = getProcessArguments();
        private static final Consumer<TransformationServiceDecorator> onInitalize = getOnInitalize();
        //private static final Consumer<TransformationServiceDecorator> runScan = getRunScan();
        private static final OnCompleteScan onCompleteScan = getOnCompleteScan();

        private interface NewWrapper extends Function<ITransformationService, TransformationServiceDecorator> { }
        private static NewWrapper getWrapper() {
            return wrap(() -> {
                var ctr = TransformationServiceDecorator.class.getDeclaredConstructor(ITransformationService.class);
                UnsafeHacks.setAccessible(ctr);
                return (NewWrapper)(service -> wrap(() -> ctr.newInstance(service)).get());
            }).get();
        }

        private interface OnLoad {
            void call(TransformationServiceDecorator instance, IEnvironment env, Set<String> otherServices);
        }

        private static OnLoad getOnLoad() {
            return wrap(() -> {
                var mtd = TransformationServiceDecorator.class.getDeclaredMethod("onLoad", IEnvironment.class, Set.class);
                UnsafeHacks.setAccessible(mtd);
                return (OnLoad)((inst, env, other) -> wrap(() -> mtd.invoke(inst, env, other)).get());
            }).get();
        }

        private static Predicate<TransformationServiceDecorator> getIsValid() {
            return wrap(() -> {
                var mtd = TransformationServiceDecorator.class.getDeclaredMethod("isValid");
                UnsafeHacks.setAccessible(mtd);
                return (Predicate<TransformationServiceDecorator>)(inst -> wrap(() -> (boolean)mtd.invoke(inst)).get());
            }).get();
        }

        private static Supplier<Object> getProcessArguments() {
            return wrap(() -> {
                var mtd = handler.getClass().getDeclaredMethod("processArguments", ArgumentHandler.class, Environment.class);
                UnsafeHacks.setAccessible(mtd);
                return wrap(() -> mtd.invoke(handler, arguments, Launcher.INSTANCE.environment()));
            }).get();
        }

        private static Consumer<TransformationServiceDecorator> getOnInitalize() {
            return wrap(() -> {
                var mtd = TransformationServiceDecorator.class.getDeclaredMethod("onInitialize", IEnvironment.class);
                UnsafeHacks.setAccessible(mtd);
                return (Consumer<TransformationServiceDecorator>)(inst -> wrap(() -> mtd.invoke(inst, Launcher.INSTANCE.environment())).get());
            }).get();
        }

        /*
        private static Consumer<TransformationServiceDecorator> getRunScan() {
            return wrap(() -> {
                var mtd = TransformationServiceDecorator.class.getDeclaredMethod("runScan", IEnvironment.class);
                UnsafeHacks.setAccessible(mtd);
                return (Consumer<TransformationServiceDecorator>)(inst -> wrap(() -> mtd.invoke(inst, Launcher.INSTANCE.environment())));
            }).get();
        }
        */

        private interface OnCompleteScan {
            List<Resource> call(TransformationServiceDecorator instance, IModuleLayerManager manager);
        }

        @SuppressWarnings("unchecked")
        private static OnCompleteScan getOnCompleteScan() {
            return wrap(() -> {
                var mtd = TransformationServiceDecorator.class.getDeclaredMethod("onCompleteScan", IModuleLayerManager.class);
                UnsafeHacks.setAccessible(mtd);
                return (OnCompleteScan)((inst, layers) -> wrap(() -> (List<Resource>)mtd.invoke(inst, layers)).get());
            }).get();
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
                "type", "PLUGINSERVICE",
                "file", file
            ));
        }
    }
}
