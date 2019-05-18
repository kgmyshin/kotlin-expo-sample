/*
 * Copyright JetBrains
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kgmyshin.kotlin.expo.utils

import com.kgmyshin.kotlin.expo.tasks.UnpackGradleDependenciesTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleJavaTargetExtension
import org.jetbrains.kotlin.gradle.plugin.Kotlin2JsPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import java.util.logging.Logger
import kotlin.reflect.full.declaredMemberProperties

internal const val pluginName = "kotlin-frontend-plugin"
internal val logger = Logger.getLogger(pluginName)

/**
 * Extracted class for support new MPP
 * (Should not be loaded on Kotlin < 1.2.70)
 */
object KotlinNewMpp {
    private val Project.kotlinExtension: KotlinProjectExtension
        get() = extensions.getByName("kotlin") as KotlinProjectExtension

    private val Project.multiplatformExtension: KotlinMultiplatformExtension?
        get() = kotlinExtension as? KotlinMultiplatformExtension

    private val KotlinSingleJavaTargetExtension.internalTarget: KotlinTarget
        get() = KotlinSingleJavaTargetExtension::class.declaredMemberProperties.find { it.name == "target" }!!.get(this) as KotlinTarget

    fun forEachJsTargetCompilationTasks(project: Project, action: (kotlin2js: Task, testKotlin2js: Task) -> Unit) {
        forEachJsTarget(project) { mainCompilation, testCompilation ->
            val main = requireKotlin2JsCompile(project, mainCompilation.compileKotlinTaskName)
            val test = requireKotlin2JsCompile(project, testCompilation.compileKotlinTaskName)

            if (main != null && test != null) {
                action(main, test)
            }
        }
    }

    private fun forEachJsTarget(
        project: Project,
        action: (kotlin2js: KotlinCompilation<*>, testKotlin2js: KotlinCompilation<*>) -> Unit
    ) {
        val kotlinExtension = project.multiplatformExtension ?: run {
            project.pluginManager.apply(Kotlin2JsPluginWrapper::class.java)
            project.kotlinExtension as KotlinSingleJavaTargetExtension
        }

        var hasTarget = false

        fun processTarget(target: KotlinTarget, name: String?) {
            if (hasTarget) {
                logger.warning("$pluginName supports only one js target. Target \"$name\" is ignored")
                return
            }

            hasTarget = true

            var mainCompilation: KotlinCompilation<*>? = null
            var testCompilation: KotlinCompilation<*>? = null

            target.compilations.all { compilation ->
                when (compilation.name) {
                    KotlinCompilation.MAIN_COMPILATION_NAME -> mainCompilation = compilation
                    KotlinCompilation.TEST_COMPILATION_NAME -> testCompilation = compilation
                    else -> logger.warning(
                        "Unsupported compilation ${name?.let { "$it." }}${compilation.name}. " +
                                "Only \"main\" and \"test\" compilations are supported by $pluginName"
                    )
                }
            }

            if (mainCompilation == null || testCompilation == null) {
                logger.severe("$pluginName requires both \"main\" and \"tests\" compilations")
                return
            }

            action(mainCompilation!!, testCompilation!!)
        }

        when (kotlinExtension) {
            is KotlinSingleJavaTargetExtension -> processTarget(kotlinExtension.internalTarget, null)
            is KotlinMultiplatformExtension ->
                kotlinExtension.targets
                    .matching { it.platformType == KotlinPlatformType.js }
                    .all { processTarget(it, it.targetName) }
        }
    }

    private fun requireKotlin2JsCompile(project: Project, name: String?): Kotlin2JsCompile? {
        if (name == null) return null
        val task = project.tasks.getByName(name) as? Kotlin2JsCompile
        if (task == null) {
            logger.severe("$pluginName requires task '$name' with type Kotlin2JsCompile")
            return null
        }
        return task
    }

    fun configureNpmCompileConfigurations(task: UnpackGradleDependenciesTask) {
        forEachJsTarget(task.project) { mainCompilation, testCompilation ->
            task.customCompileConfiguration =
                task.project.configurations.getByName(mainCompilation.compileDependencyConfigurationName)
            task.customTestCompileConfiguration =
                task.project.configurations.getByName(testCompilation.compileDependencyConfigurationName)
        }
    }
}