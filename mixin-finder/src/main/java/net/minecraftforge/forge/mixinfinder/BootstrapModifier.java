/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.forge.mixinfinder;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import net.minecraftforge.bootstrap.api.BootstrapClasspathModifier;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Mixin provides a ILaunchPluginService. Which is only located via the boot layer.
 * So we need to enter super early and swap out what version we want to use.
 * Right now, it compares version numbers, picking the highest.
 * If it finds a version in the mods folder it picks that over any others.
 */
@ApiStatus.Internal
public class BootstrapModifier implements BootstrapClasspathModifier {
    private static final Pattern DASH_VERSION = Pattern.compile("-(\\d+(\\.|$))");
    private Path gameDir = Path.of(".").toAbsolutePath().normalize();

    @Override
    public String name() {
        return "mixin-finder";
    }

    public String[] arguments(String[] args) {
        for (int x = 0; x < args.length; x++) {
            if (args[x].startsWith("--gameDir=")) {
                gameDir = Path.of(args[x].substring(10));
                break;
            } else if (args[x].equals("--gameDir") && x + 1 < args.length) {
                gameDir = Path.of(args[x + 1]).toAbsolutePath().normalize();
                break;
            }
        }

        return args;
    }

    private record Located(@Nullable Path[] classpath, Path path, @Nullable Version version) {}

    @Override
    public boolean process(List<Path[]> classpath) {
        List<Located> located = new ArrayList<>();
        for (int x = 0; x < classpath.size(); x++) {
            var paths = classpath.get(x);
            if (paths.length != 1)
                continue;

            var info = getMixinInfo(paths[0], paths);
            if (info != null)
                located.add(info);
        }

        boolean modsDirFound = false;
        var modsDir = gameDir.resolve("mods");
        if (Files.exists(modsDir)) {
            try {
                for (var path : Files.list(modsDir).toList()) {
                    var info = getMixinInfo(path, null);
                    if (info != null) {
                        located.add(info);
                        modsDirFound = true;
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to read mods directory: " + modsDir);
                e.printStackTrace(System.err);
            }
        }

        Located selected = null;
        for (var option : located) {
            // If we found something in the mods dir, assume they want that, and only use it.
            if (modsDirFound && option.classpath != null)
                continue;

            if (selected == null || higherVersion(selected, option))
                selected = option;
        }

        boolean ret = false;
        System.out.println("Found Mixins:");
        for (var mixin : located) {
            if (selected == mixin) {
                System.out.println("\t*" + mixin.version + ": " + mixin.path().toAbsolutePath());
                if (mixin.classpath == null) {
                    classpath.add(new Path[] { mixin.path });
                    ret = true;
                }
            } else {
                System.out.println("\t " + mixin.version + ": " + mixin.path().toAbsolutePath());
                if (mixin.classpath != null) {
                    classpath.removeIf(mixin.classpath::equals);
                    ret = true;
                }
            }
        }

        return ret;
    }

    private static Located getMixinInfo(Path path, Path[] classpath) {
        if (Files.isDirectory(path) || path.getFileName() == null)
            return null;

        var name = path.getFileName().toString().toLowerCase(Locale.ENGLISH);
        if (!name.endsWith(".jar") || !name.contains("mixin"))
            return null;

        try (FileSystem fs = FileSystems.newFileSystem(path)) {
            var moduleInfoPath = fs.getPath("/module-info.class");
            if (!Files.exists(moduleInfoPath))
                return null;

            try (var reader = Files.newInputStream(moduleInfoPath)) {
                var module = ModuleDescriptor.read(reader);

                if (!"org.spongepowered.mixin".equals(module.name()))
                    return null;

                var version = module.version().orElse(null);
                if (version == null) {
                    var matcher = DASH_VERSION.matcher(name);
                    if (matcher.find()) {
                        // attempt to parse the tail as a version string
                        try {
                            var tail = name.substring(matcher.start() + 1, name.length() - 4);
                            version = Version.parse(tail);
                        } catch (IllegalArgumentException ignore) { }
                    }
                }

                return new Located(classpath, path, version);
            }
        } catch (IOException e) {
            System.err.println("Failed to read jar file: " + path);
            e.printStackTrace(System.err);
            return null;
        }
    }

    private static boolean higherVersion(Located o1, Located o2) {
        if (o1.version() != null && o2.version() == null)
            return false;
        if (o1.version() == null)
            return o2.version() != null;
        int cmp = o1.version().compareTo(o2.version());
        return cmp > 0;
    }
}
