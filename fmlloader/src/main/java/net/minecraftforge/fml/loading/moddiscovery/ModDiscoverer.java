/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.loading.moddiscovery;

import com.mojang.logging.LogUtils;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.Launcher;
import net.minecraftforge.fml.loading.EarlyLoadingException;
import net.minecraftforge.fml.loading.EarlyLoadingException.ExceptionData;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LogMarkers;
import net.minecraftforge.fml.loading.UniqueModListBuilder;
import net.minecraftforge.fml.loading.progress.StartupNotificationManager;
import net.minecraftforge.forgespi.Environment;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.locating.IDependencyLocator;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModFile.Type;
import net.minecraftforge.securemodules.SecureModuleClassLoader;
import net.minecraftforge.securemodules.SecureModuleFinder;
import net.minecraftforge.forgespi.locating.IModLocator;
import net.minecraftforge.forgespi.locating.IModProvider;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

@ApiStatus.Internal
public final class ModDiscoverer {
    private ModDiscoverer() {}

    private static final Logger LOGGER = LogUtils.getLogger();

    public static ModValidator discoverMods(Map<String, ?> arguments, List<Path> extraLocators) {
        var env = Launcher.INSTANCE.environment();
        env.computePropertyIfAbsent(Environment.Keys.MODDIRECTORYFACTORY.get(), v -> ModsFolderLocator::new);
        env.computePropertyIfAbsent(Environment.Keys.PROGRESSMESSAGE.get(), v -> StartupNotificationManager.locatorConsumer().orElseGet(()-> s->{}));

        var serviceLayer = ModDiscoverer.class.getModule().getLayer();
        if (!extraLocators.isEmpty()) {
            var jars = new ArrayList<SecureJar>(extraLocators.size());
            var targets = new ArrayList<String>(extraLocators.size());
            for (var path : extraLocators) {
                var jar = SecureJar.from(path);
                jars.add(jar);
                targets.add(jar.name());
            }

            var cfg = serviceLayer.configuration().resolveAndBind(SecureModuleFinder.of(jars), ModuleFinder.of(), targets);
            var cl = new SecureModuleClassLoader("LOCATORS", ModDiscoverer.class.getClassLoader(), cfg, List.of(serviceLayer), List.of());
            serviceLayer = serviceLayer.defineModules(cfg, module -> cl);
        }

        var modLocators = initLocators(serviceLayer, IModLocator.class, "Mods", arguments);

        LOGGER.debug(LogMarkers.SCAN, "Scanning for mods and other resources to load. We know {} ways to find mods", modLocators.size());

        List<ModFile> loadedFiles = new ArrayList<>();
        List<ExceptionData> discoveryErrorData = new ArrayList<>();
        List<IModFileInfo> brokenFiles = new ArrayList<>();
        boolean distIsDedicatedServer = FMLLoader.getDist().isDedicatedServer();

        //Loop all mod locators to get the prime mods to load from.
        for (IModLocator locator : modLocators) {
            try {
                LOGGER.debug(LogMarkers.SCAN, "Trying locator {}", locator);
                var candidates = locator.scanMods();

                if (candidates.isEmpty())
                    continue;

                int exceptions = 0;
                int located = 0;

                for (var candidate : candidates) {
                    if (candidate.ex() != null) {
                        exceptions++;

                        // pipe exception messages through the discoveryErrorData to avoid swallowing some exceptions and improve error messages
                        // (no longer a generic "Invalid mod file" for all InvalidModExceptions - it actually shows the exception message now)
                        if (candidate.ex() instanceof InvalidModFileException ex) {
                            brokenFiles.add(ex.getBrokenFile());
                            discoveryErrorData.add(new ExceptionData(candidate.ex().getMessage(), ex.getBrokenFile()));
                        } else {
                            discoveryErrorData.add(new ExceptionData(candidate.ex().getMessage()));
                        }
                        continue;
                    } else {
                        var file = candidate.file();
                        var info = file.getModFileInfo();

                        if (distIsDedicatedServer && info.isClientSideOnly()) {
                            LOGGER.debug(LogMarkers.SCAN, "Skipping mod file {}, as it is client side only and we are a dedicated server!", file.getFileName(), locator);
                        } else if (file instanceof ModFile mf) {
                            located++;
                            LOGGER.debug(LogMarkers.SCAN, "Found mod file {} of type {} with provider {}", mf.getFileName(), mf.getType(), mf.getProvider());
                            loadedFiles.add(mf);
                        } else {
                            brokenFiles.add(info);
                            LOGGER.error(LogMarkers.SCAN, "Skipping mod file {} found by {}, as it was not a ModFile instance!", file.getFileName(), locator);
                        }
                    }
                }

                LOGGER.info(LogMarkers.SCAN, "Locator {} found {} candidates, {} errors, and loaded {}", locator, candidates.size() - exceptions, exceptions, located);
            } catch (InvalidModFileException imfe) {
                // We don't generally expect this exception, since it should come from the candidates stream above and be handled in the Locator, but just in case.
                LOGGER.error(LogMarkers.SCAN, "Locator {} found an invalid mod file {}", locator, imfe.getBrokenFile(), imfe);
                brokenFiles.add(imfe.getBrokenFile());
            } catch (EarlyLoadingException exception) {
                LOGGER.error(LogMarkers.SCAN, "Failed to load mods with locator {}", locator, exception);
                discoveryErrorData.addAll(exception.getAllData());
            }
        }

        //First processing run of the mod list. Any duplicates will cause resolution failure and dependency loading will be skipped.
        try {
            loadedFiles = UniqueModListBuilder.build(loadedFiles);
            if (loadedFiles.isEmpty()) {
                LOGGER.debug(LogMarkers.SCAN, "Located 0 mods, skipping dependency locators.");
            } else {
                LOGGER.debug(LogMarkers.SCAN, "Successfully Loaded {} mods. Attempting to load dependencies...", loadedFiles.size());
                loadedFiles = loadDependencies(serviceLayer, arguments, loadedFiles, brokenFiles, discoveryErrorData);
            }
        } catch (EarlyLoadingException exception) {
            LOGGER.error(LogMarkers.SCAN, "Failed to build unique mod list after mod discovery. Skipping dependency discovery.", exception);
            discoveryErrorData.addAll(exception.getAllData());
        }

        // Now build the new mod layers map
        var modFilesMap = new EnumMap<Type, List<ModFile>>(Type.class);
        for (var file : loadedFiles)
            modFilesMap.computeIfAbsent(file.getType(), k -> new ArrayList<>()).add(file);

        //Validate the loading. With a deduplicated list, we can now successfully process the artifacts and load
        //transformer plugins.
        var validator = new ModValidator(modFilesMap, brokenFiles, discoveryErrorData, List.of());
        validator.stage1Validation();
        return validator;
    }

    private static List<ModFile> loadDependencies(ModuleLayer serviceLayer, Map<String, ?> arguments, List<ModFile> primaryMods, List<IModFileInfo> broken, List<ExceptionData> errors) {
        var locators = initLocators(serviceLayer, IDependencyLocator.class, "Dependency", arguments);

        // We create a copy to not modify the 'prime' mod list until we finish and sort them.
        List<ModFile> mods = new ArrayList<>(primaryMods);
        List<IModFile> modsView = Collections.unmodifiableList(mods);

        for (var locator : locators) {
            try {
                LOGGER.debug(LogMarkers.SCAN, "Trying locator {}", locator);
                var candidates = locator.scanMods(modsView);
                for (var candidate : candidates) {
                    if (candidate instanceof ModFile mf) {
                        mods.add(mf);
                        LOGGER.debug(LogMarkers.SCAN, "Found mod file {} of type {} with provider {}", mf.getFileName(), mf.getType(), mf.getProvider());
                    } else {
                        broken.add(candidate.getModFileInfo());
                        LOGGER.error(LogMarkers.SCAN, "Skipping mod file {} found by {}, as it was not a ModFile instance!", candidate.getFileName(), locator);
                    }
                }
            } catch (EarlyLoadingException exception) {
                LOGGER.error(LogMarkers.SCAN, "Failed to load dependencies with locator {}", locator, exception);
                errors.addAll(exception.getAllData());
            }
        }

        //Second processing run of the mod list. Any duplicates will cause resolution failure and we'll return the input list.
        try {
            return UniqueModListBuilder.build(mods);
        } catch (EarlyLoadingException exception) {
            LOGGER.error(LogMarkers.SCAN, "Failed to build unique mod list after dependency discovery.", exception);
            errors.addAll(exception.getAllData());
            return primaryMods;
        }
    }

    private static <T extends IModProvider> List<T> initLocators(ModuleLayer layer, Class<T> clazz, String type, Map<String, ?> arguments) {
        var loader = ServiceLoader.load(layer, clazz);
        var ret = new ArrayList<T>();

        for (var itr = loader.iterator(); itr.hasNext(); ) {
            try {
                var service = itr.next();
                service.initArguments(arguments);
                ret.add(service);
            } catch (ServiceConfigurationError e) {
                LOGGER.error("Failed to load {} locator", e);
            }
        }

        if (LOGGER.isDebugEnabled(LogMarkers.CORE)) {
            LOGGER.debug(LogMarkers.CORE, "Found {} Locators: {}", type, ret.stream()
                .map(l -> "(%s:%s)".formatted(l.name(), getVersion(l)))
                .collect(Collectors.joining(",")));
        }

        return ret;
    }

    private static String getVersion(Object obj) {
        var cls = obj.getClass();
        var ret = cls.getPackage().getImplementationVersion();
        if (ret == null)
            ret = cls.getModule().getDescriptor().rawVersion().orElse(null);
        return ret;
    }
}
