/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.loading;

import com.mojang.logging.LogUtils;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.modlauncher.ArgumentHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.api.TypesafeMap;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.Environment;
import net.minecraftforge.unsafe.UnsafeHacks;

import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static net.minecraftforge.fml.loading.LogMarkers.CORE;
import static net.minecraftforge.fml.loading.LogMarkers.SCAN;

@ApiStatus.Internal
public class ModDiscoveryService implements ITransformerDiscoveryService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> SERVICES = Set.of(
        "cpw.mods.modlauncher.api.ITransformationService"
    );
    private static final Set<String> LOCATORS = Set.of(
        "net.minecraftforge.forgespi.locating.IModLocator",
        "net.minecraftforge.forgespi.locating.IDependencyLocator"
    );

    private static volatile Set<Path> claimed = Set.of();
    static IncompatibleEnvironmentException onLoadException;

    public ModDiscoveryService() {
        var markers = System.getProperty("forge.logging.markers", "").split(",");
        for (var marker : markers)
            System.setProperty("forge.logging.marker." + marker.toLowerCase(Locale.ROOT), "ACCEPT");
    }

    @Override
    public List<NamedPath> candidates(final Path gameDirectory) {
        throw new IllegalStateException("ModLauncher does not use this method directly");
    }

    @Override
    public void earlyInitialization(final String launchTarget, final String[] arguments) {
        ImmediateWindowHandler.load(launchTarget, arguments);
    }

    public static boolean isClaimed(Path path) {
        return claimed.contains(path);
    }

    public static boolean isService(String name) {
        return SERVICES.contains(name);
    }

    @Override
    public List<NamedPath> candidates(final Path gameDirectory, final String launchTarget) {
        LOGGER.debug(CORE, "Setting up basic FML game directories");
        FMLPaths.loadAbsolutePaths(gameDirectory);
        LOGGER.debug(CORE, "Loading configuration");
        FMLConfig.load();

        var environment = (IEnvironment)Launcher.INSTANCE.environment();
        // These are the things we need to do before we do any discovery, basically the first few parts of FMLServiceProvider's ITransformationServince
        // 1 : onLoad
        try {
            FMLLoader.onInitialLoad(environment);
        } catch (IncompatibleEnvironmentException e) {
            // Capture it so we can yell at ModLauncher at the right time.
            onLoadException = e;
            return List.of();
        }

        // 2 : initialize
        final Map<String, Object> arguments = new HashMap<>(); // This is only ever  { launchTarget: "launchTarget" } but its mutable so locators might add to it.
        LOGGER.debug(CORE, "Preparing ModFile");
        environment.computePropertyIfAbsent(Environment.Keys.MODFILEFACTORY.get(), k -> ModFile::new);
        LOGGER.debug(CORE, "Preparing launch handler");
        FMLLoader.setupLaunchHandler(launchTarget, arguments); // This sets the laucnhTarget variable in arguments
        FMLEnvironment.setupInteropEnvironment(environment);
        Environment.build(environment);

        // Now we can actually start searching for mods, lets locate any transformers or locators
        var results = scan(FMLPaths.MODSDIR.get().toAbsolutePath().normalize());

        // Set the flat to enable mixin if we find it in the process arguments, this is used by JarInJarDependencyLocator
        Launcher.INSTANCE.blackboard().putIfAbsent(
            TypesafeMap.Key.getOrCreate(Launcher.INSTANCE.blackboard(), "isMixinEnabledInDev", Boolean.class),
            LazyArgs.isMixinInArgs
        );

        // 3: beginScanning: Finally, actually start scanning
        LOGGER.debug(CORE,"Initiating mod scan");
        FMLLoader.beginModScan(arguments, results.locators());

        // Gather any services found by locators
        var ret = new ArrayList<NamedPath>();
        var type = SERVICES.iterator().next(); // Doesn't matter, just needs a name for the debug log
        var seen = new HashSet<String>();
        for (var jar : FMLLoader.getServiceResources().resources()) {
            // This is annoying and makes things only work against single file secure jars.
            // But in our case I think thats fine.
            // I would need to edit ModLauncher to take in SecureJar directly like it does for other layers.
            // Or allow SecureJar.from to figure out how to reuse paths that point to existing secure jars.
            var path = jar.getPrimaryPath();
            ret.add(new NamedPath(type, path));
            seen.add(jar.moduleDataProvider().name());
        }

        // Add any we found here, that wern't found by locators
        for (var e : results.services().entrySet()) {
            if (seen.add(e.getKey()))
                ret.add(e.getValue());
        }

        return ret;
    }

    private record ScanResults(List<Path> locators, Map<String, NamedPath> services) {}
    private ScanResults scan(Path directory) {
        // Skip if the mods dir doesn't exist yet.
        if (!Files.exists(directory))
            return new ScanResults(List.of(), Map.of());

        var claimed = new HashSet<Path>();
        var locators = new ArrayList<Path>();
        var named = new HashMap<String, NamedPath>();

        try (var walk = Files.walk(directory, 1)) {
            var services = new HashSet<String>();
            for (var paths = walk.iterator(); paths.hasNext(); ) {
                var path = paths.next();
                if (!Files.isRegularFile(path) || !path.toString().endsWith(".jar") || LamdbaExceptionUtils.uncheck(() -> Files.size(path)) == 0)
                    continue;

                String moduleName = null;
                services.clear();
                // Making a SecureJar is useful, but heavy, so lets just read as a zip
                try (var zip = FileSystems.newFileSystem(path)) {
                    var root = zip.getRootDirectories().iterator().next();
                    var modulePath = root.resolve("module-info.class");
                    var servicesPath = root.resolve("META-INF/services/");

                    if (Files.exists(modulePath)) {
                        try (var stream  = Files.newInputStream(modulePath)) {
                            var desc = ModuleDescriptor.read(stream);
                            moduleName = desc.name();
                            for (var provider : desc.provides()) {
                                services.add(provider.service());
                            }
                        }
                    } else if (Files.exists(servicesPath)) {
                        var manifest = root.resolve(JarFile.MANIFEST_NAME);
                        if (Files.exists(manifest)) {
                            try (var is = Files.newInputStream(manifest)) {
                                var mf =  new Manifest(is);
                                moduleName = mf.getMainAttributes().getValue("Automatic-Module-Name");
                            }
                        }

                        for (var itr = Files.list(servicesPath).iterator(); itr.hasNext(); ) {
                            var file = itr.next();
                            if (!Files.isRegularFile(file))
                                continue;
                            services.add(file.getFileName().toString());
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error(SCAN, "Failed to scan " + path, e);
                }

                if (services.isEmpty())
                    continue;

                var name = services.stream().filter(LOCATORS::contains).findFirst();
                if (name.isPresent())
                    locators.add(path);
                else
                    name = services.stream().filter(SERVICES::contains).findFirst();

                if (name.isPresent()) {
                    claimed.add(path);
                    if (moduleName == null)
                        moduleName = JarMetadata.fromFileName(path, Set.of(), List.of()).descriptor().name();
                    named.put(moduleName, new NamedPath(name.get(), path));
                }
            }
        } catch (IOException | IllegalStateException ioe) {
            LOGGER.error(SCAN, "Error during early discovery", ioe);
        }

        ModDiscoveryService.claimed = claimed;
        return new ScanResults(locators, named);
    }

    private static class LazyArgs {
        private static final String[] args = args();
        private static final boolean isMixinInArgs = isMixinInArgs();

        private static String[] args() {
            var handler = UnsafeHacks.<Launcher, ArgumentHandler>findField(Launcher.class, "argumentHandler").get(Launcher.INSTANCE);
            return UnsafeHacks.<ArgumentHandler, String[]>findField(ArgumentHandler.class, "args").get(handler);
        }

        private static boolean isMixinInArgs() {
            for (String arg : args) {
                if (arg.startsWith("-mixin.config"))
                    return true;
            }
            return false;
        }
    }
}
