package net.minecraftforge.forge.build;

import net.minecraftforge.gitversion.gradle.GitVersionExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;

import javax.inject.Inject;

public abstract class VersionsExtension {
    private final VersionCatalog libs;

    public final String java, minecraft, forge, minecraftNext, mcp, changelogBase;

    @Inject
    public VersionsExtension(Project project) {
        final var extensions = project.getExtensions();
        final var gitversion = extensions.getByType(GitVersionExtension.class);
        libs = extensions.getByType(VersionCatalogsExtension.class).named("bootLibs");
        java          = get("java");
        minecraft     = get("minecraft");
        minecraftNext = get("minecraft.next");
        mcp           = get("mcp");
        changelogBase = get("changelog.base");
        forge         = gitversion.of(project.getRootProject()).getVersion();
        project.setVersion(minecraft + '-' + gitversion.getVersion());
    }

    private String get(String name) {
        var ver = this.libs.findVersion(name);
        if (ver.isEmpty())
            throw new IllegalStateException("No version found for " + name);
        return ver.get().toString();
    }
}
