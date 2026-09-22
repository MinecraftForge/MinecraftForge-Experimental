package net.minecraftforge.forge.build

import groovy.transform.CompileStatic
import net.minecraftforge.forgedev.ForgeDevExtension
import net.minecraftforge.gitversion.gradle.GitVersionExtension
import net.minecraftforge.gitversion.gradle.changelog.ChangelogExtension
import net.minecraftforge.gradleutils.GradleUtilsExtensionForProject
import net.minecraftforge.licenser.LicenseExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension

@CompileStatic
interface Meta {
    Project getProject();

    default VersionCatalog getLibs() {
        project.extensions.getByType(VersionCatalogsExtension).named("libs")
    }

    default ExtraPropertiesExtension getExt() {
        project.extensions.getByType(ExtraPropertiesExtension)
    }

    default JavaPluginExtension getJava() {
        project.extensions.getByType(JavaPluginExtension)
    }

    default GradleUtilsExtensionForProject getGradleutils() {
        project.extensions.getByType(GradleUtilsExtensionForProject)
    }

    default GitVersionExtension getGitversion() {
        project.extensions.getByType(GitVersionExtension)
    }

    default ForgeDevExtension getForgedev() {
        project.extensions.getByType(ForgeDevExtension)
    }

    default ChangelogExtension getChangelog() {
        project.extensions.getByType(ChangelogExtension)
    }

    default LicenseExtension getLicense() {
        project.extensions.getByType(LicenseExtension)
    }

    default PublishingExtension getPublishing() {
        project.extensions.getByType(PublishingExtension)
    }

    default VersionsExtension getVersions() {
        project.extensions.getByType(VersionsExtension)
    }

    default ConventionExtension getConvention() {
        project.extensions.getByType(ConventionExtension)
    }

    default String pluginId(String alias) {
        getLibs().findPlugin(alias).orElseThrow { new IllegalStateException("Could not find plugin " + alias) }.get().pluginId
    }
}
