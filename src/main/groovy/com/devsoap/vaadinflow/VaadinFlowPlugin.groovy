/*
 * Copyright 2018 Devsoap Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.vaadinflow

import com.devsoap.vaadinflow.actions.GrettyPluginAction
import com.devsoap.vaadinflow.actions.NodePluginAction
import com.devsoap.vaadinflow.actions.PluginAction
import com.devsoap.vaadinflow.actions.VaadinFlowPluginAction
import com.devsoap.vaadinflow.actions.WarPluginAction
import com.devsoap.vaadinflow.extensions.VaadinClientDependenciesExtension
import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import com.devsoap.vaadinflow.tasks.AssembleClientDependenciesTask
import com.devsoap.vaadinflow.tasks.ConvertCssToHtmlStyleTask
import com.devsoap.vaadinflow.tasks.CreateComponentTask
import com.devsoap.vaadinflow.tasks.CreateCompositeTask
import com.devsoap.vaadinflow.tasks.CreateProjectTask
import com.devsoap.vaadinflow.tasks.CreateWebComponentTask
import com.devsoap.vaadinflow.tasks.InstallBowerDependenciesTask
import com.devsoap.vaadinflow.tasks.InstallYarnDependenciesTask
import com.devsoap.vaadinflow.tasks.TranspileDependenciesTask
import com.devsoap.vaadinflow.util.Versions
import groovy.util.logging.Log
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.invocation.Gradle
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.util.VersionNumber

/**
 * Main plugin class
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class VaadinFlowPlugin implements Plugin<Project> {

    static final String PLUGIN_ID = 'com.devsoap.vaadin-flow'

    private final List<PluginAction> actions = []

    VaadinFlowPlugin() {
        actions << new VaadinFlowPluginAction()
        actions << new NodePluginAction()
        actions << new WarPluginAction()
        actions << new GrettyPluginAction()
    }

    @Override
    void apply(Project project) {
        project.with {
            validateGradleVersion(it)

            actions.each { action ->
                action.apply(project)
            }

            extensions.with {
                create(VaadinFlowPluginExtension.NAME, VaadinFlowPluginExtension, project)
                create(VaadinClientDependenciesExtension.NAME, VaadinClientDependenciesExtension, project)
            }

            tasks.with {
                create(CreateProjectTask.NAME, CreateProjectTask)
                create(CreateWebComponentTask.NAME, CreateWebComponentTask)
                create(InstallYarnDependenciesTask.NAME, InstallYarnDependenciesTask)
                create(InstallBowerDependenciesTask.NAME, InstallBowerDependenciesTask)
                create(TranspileDependenciesTask.NAME, TranspileDependenciesTask)
                create(AssembleClientDependenciesTask.NAME, AssembleClientDependenciesTask)
                create(ConvertCssToHtmlStyleTask.NAME, ConvertCssToHtmlStyleTask)
                create(CreateCompositeTask.NAME, CreateCompositeTask)
                create(CreateComponentTask.NAME, CreateComponentTask)
            }

            workaroundInvalidBomVersionRanges(it)

            afterEvaluate {
                disableStatistics(project)
            }
        }
    }

    /**
     * Looks like vaadins BOM is using invalid version ranges which do not account for alpha/beta releases.
     * Workaround it here by using the '+' notation.
     *
     * FIXME This is a ugly hack that should be removed once Vaadin gets its BOMs fixed.
     *
     * @param project
     *      the project
     */
    private static void workaroundInvalidBomVersionRanges(Project project) {
        project.configurations.all {
            it.resolutionStrategy.eachDependency { dep ->
                if (dep.requested.group == 'org.webjars.bowergithub.vaadin'
                        && dep.requested.name != 'vaadin-usage-statistics'
                        && dep.requested.version.startsWith('[')) {
                    dep.because("Dependency is using a unsupported range")
                    dep.useVersion("latest.release")
                }
                if(dep.requested.group == 'org.webjars.bowergithub.vaadin' && dep.requested.name == 'vaadin-lumo-styles') {
                    // Do not pull in lumo-styles Polymer 3 alpha/beta components
                    dep.useVersion("1.0.0")
                }
                if(dep.requested.group == 'org.webjars.bowergithub.webcomponents' && dep.requested.name == 'shadycss') {
                    // Lock shadycss (used by lumo-styles) as 1.3.x does no longer support bower
                    dep.useVersion("1.2.1")
                }
            }
        }
    }

    private static void disableStatistics(Project project) {
        VaadinFlowPluginExtension vaadin = project.extensions[VaadinFlowPluginExtension.NAME]
        if (!vaadin.submitStatistics) {
            Dependency statistics = vaadin.disableStatistics()
            project.configurations['implementation'].dependencies.add(statistics)
            project.configurations.all { config ->
                config.resolutionStrategy.force("${statistics.group}:${statistics.name}:${statistics.version}")
            }
        }
    }

    private static void validateGradleVersion(Project project) {
        Gradle gradle = project.gradle
        VersionNumber version = VersionNumber.parse(gradle.gradleVersion)
        VersionNumber requiredVersion = Versions.version('vaadin.plugin.gradle.version')
        if ( version.baseVersion < requiredVersion ) {
            throw new UnsupportedVersionException("Your gradle version ($version) is too old. " +
                    "Plugin requires Gradle $requiredVersion+")
        }
    }
}
