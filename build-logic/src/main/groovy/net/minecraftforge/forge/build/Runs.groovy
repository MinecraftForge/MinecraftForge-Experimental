package net.minecraftforge.forge.build

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.jspecify.annotations.Nullable

import javax.inject.Inject

@CompileStatic
class Runs implements Meta {
    private final ConventionExtension ext

    @Inject
    Runs(ConventionExtension ext) {
        this.ext = ext
    }

    @Override
    Project getProject() {
        this.ext.project
    }

    void apply(@Nullable String mods, @Nullable File output) {
        final project = this.project
        final genAllData = this.ext.genAllData

        project.with {
            if (project != project.rootProject) {
                dependencies.with {
                    add('implementation', it.project(':')) // Root project is forge itself
                }
            }

            forgedev.runs.with {
                final File existing = getJava().getSourceSets().named("main").get().getResources().getSrcDirs()[0];
                configureEach {
                    it.with {
                        cache.set(project.layout.projectDirectory.dir('runs/cache'))
                        eclipsePrefix = project.name

                        // If we're not forge, we need to get the metadata from the forge project
                        if (project != project.rootProject)
                            metadata = project.files(getForgedev().consumeFile('metadata', project.rootProject))


                        var runName = it.name.toLowerCase(Locale.ENGLISH)

                        if (runName.contains('client') || runName.contains('data')) {
                            options.with {
                                args '--assetsDir', '{assets_root}', '--assetIndex', '{asset_index}'
                                systemProperty 'org.lwjgl.system.SharedLibraryExtractDirectory', 'lwjgl_dll'
                            }
                        }

                        if (runName.contains('data')) {
                            if (mods != null)   options.args('--mod', mods)
                            if (output != null) options.args('--output', output)
                        }

                        options.with {
                            getWorkingDir().set(project.layout.projectDirectory.dir("runs/$name"))
                            getMainClass().set('net.minecraftforge.bootstrap.ForgeBootstrap')
                            args '--gameDir', '.'
                            jvmArgs '-Djava.net.preferIPv6Addresses=system', '-XX:+UseCompactObjectHeaders'

                            systemProperty 'bsl.debug', 'true'
                            systemProperty 'terminal.jline', 'true'
                            systemProperty 'forge.enableGameTest', 'true'
                            systemProperty 'eventbus.api.strictRuntimeChecks', 'true'
                        }
                    }
                }

                create('client').with {
                    options.with {
                        args '--launchTarget', 'forge_dev_client',
                            '--username', 'Dev',
                            '--version', project.name,
                            '--accessToken', '0',
                            '--userType', 'mojang',
                            '--versionType', 'release'
                    }
                }

                create('server').with {
                    options.with {
                        args '--launchTarget', 'forge_dev_server'
                    }
                }

                create('gameTestServer').with {
                    options.with {
                        args '--launchTarget', 'forge_dev_server_gametest',
                            '--uniqueWorld' // Unique world is used so that the world regenerates, as well as the world config isn't influenced by other runs
                    }
                }

                create('data').with {
                    options.with {
                        args '--launchTarget', 'forge_dev_data',
                            '--all',
                            '--existing', existing
                    }
                }

                create('clientData').with {
                    options.with {
                        args '--launchTarget', 'forge_dev_client_data',
                            '--all',
                            '--existing', existing
                    }
                }
            }
            genAllData.configure {
                it.dependsOn forgedev.runs.getByName('data').run, forgedev.runs.getByName('clientData').run
            }
        }
    }
}
