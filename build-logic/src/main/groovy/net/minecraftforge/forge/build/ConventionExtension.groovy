package net.minecraftforge.forge.build

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jspecify.annotations.Nullable

import javax.inject.Inject

@CompileStatic
abstract class ConventionExtension implements Meta {
    private final Project project;
    private final Runs runs

    protected abstract @Inject ObjectFactory getObjects()
    protected abstract @Inject ProviderFactory getProviders()

    @Inject
    ConventionExtension(Project project) {
        this.project = project
        this.runs = objects.newInstance(Runs, this)
    }

    @Override
    Project getProject() {
        this.project
    }

    void manifest(String pkg) {
        this.setupManifest(pkg, null)
    }
    void langProvider(String pkg) {
        this.setupManifest(pkg, 'LANGPROVIDER')
    }
    private void setupManifest(String pkg, @Nullable String modType) {
        this.project.tasks.named('jar', Jar).configure {
            this.gradleutils.manifestDefaults(it.manifest, pkg)
            if (modType != null) {
                it.manifest.attributes([
                    'Automatic-Module-Name': pkg.replace('/', '.').substring(0, pkg.length() - 1),
                    'FMLModType'           : modType
                ])
            }
        }
    }


    // region Maven publishing ==============================================================
    void publish() {
        publish(pub -> pub.from(project.components.getByName('java')))
    }
    void publish(Action<MavenPublication> config) {
        project.with {
            publishing.with {
                repositories.with {
                    maven gradleutils.publishingForgeMaven
                }
                publications.register('mavenJava', MavenPublication) {
                    changelog.publish(it)
                    gradleutils.promote(it)

                    // The actual artifacts to be published should be configured, so run the config
                    config.execute(it)

                    it.pom.with {
                        description = project.description
                        gradleutils.pom.addRemoteDetails(it)
                        license.with {
                            gradleutils.pom.licenses.LGPLv2_1
                        }
                    }
                }
            }
        }
    }
    // endregion

    // region Runs ================================================================
    private TaskProvider<Task> genAllData = null
    TaskProvider<Task> getGenAllData() {
        if (genAllData == null)
            genAllData = this.project.tasks.register('genAllData')
        return genAllData
    }
    TaskProvider<Task> getGenAllData(Action<Task> config) {
        getGenAllData().configure(config)
        return getGenAllData()
    }

    void basicRuns() {
        this.runs.apply(null, null)
    }

    void basicRuns(String mods, File output) {
        this.runs.apply(mods, output)
    }
    // endregion

    // region Forge Version Json ===================================================
    private TaskProvider<WriteForgeVersionJson> writeForgeVersionJson = null
    TaskProvider<WriteForgeVersionJson> getForgeVersionJson() {
        if (writeForgeVersionJson == null) {
            writeForgeVersionJson = this.project.tasks.register('writeForgeVersionJson', WriteForgeVersionJson)
            final versions = this.versions
            writeForgeVersionJson.configure {
                it.forgeVersion.set(versions.forge)
                it.minecraftVersion.set(versions.minecraft)
                it.mcpVersion.set(versions.mcp)
            }
        }
        return writeForgeVersionJson
    }
    TaskProvider<WriteForgeVersionJson> forgeVersionJson(Action<WriteForgeVersionJson> config) {
        forgeVersionJson.configure(config)
        return forgeVersionJson
    }
    // endregion

    // region Natives helper =========================================================
    // See gradle/gradle#35070 (https://github.com/gradle/gradle/issues/35070)
    // We are making the list of providers first BEFORE mapping them to a provider of the resolved objects so
    // that they can be finalized after configuration.
    List<MinimalExternalModuleDependency> nativeVariants(Provider<ExternalModuleDependencyBundle> bundle) {
        final natives = "natives-" + forgedev.os
        final deps = this.project.dependencies
        bundle.get().collect { dep ->
            (deps.variantOf(providers.provider { (MinimalExternalModuleDependency)dep }) {
                it.classifier(natives)
            }).get()
        }
    }
    // endregion
}
