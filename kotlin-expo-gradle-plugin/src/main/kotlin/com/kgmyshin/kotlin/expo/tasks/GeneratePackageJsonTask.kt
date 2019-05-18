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

package com.kgmyshin.kotlin.expo.tasks

import com.kgmyshin.kotlin.expo.extensions.NpmDependency
import com.kgmyshin.kotlin.expo.extensions.NpmExtension
import com.kgmyshin.kotlin.expo.utils.npmrc
import com.kgmyshin.kotlin.expo.utils.packageJson
import com.kgmyshin.kotlin.expo.utils.toSemver
import groovy.json.JsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

open class GeneratePackageJsonTask : DefaultTask() {

    @Internal
    private val npm = project.extensions.getByType(NpmExtension::class.java)

    @Input
    val packageJsonFile = project.packageJson()

    @Input
    val npmrcFile = project.npmrc()

    override fun getGroup(): String? = "expo"

    override fun getDescription(): String? = "generate package.json"

    val mainJs =
        (project.tasks.getByPath("compileKotlin2Js") as Kotlin2JsCompile).outputFile.name

    private val defaultDependencies = listOf(
        NpmDependency(
            name = "expo",
            versionOrUri = "^32.0.6",
            scope = NpmDependency.RuntimeScope
        ),
        NpmDependency(
            name = "react",
            versionOrUri = "16.6.0",
            scope = NpmDependency.RuntimeScope
        ),
        NpmDependency(
            name = "react-native",
            versionOrUri = "https://github.com/expo/react-native/archive/sdk-32.0.0.tar.gz",
            scope = NpmDependency.RuntimeScope
        ),
        NpmDependency(
            name = "schedule",
            versionOrUri = "0.3.0",
            scope = NpmDependency.RuntimeScope
        ),
        NpmDependency(
            name = "babel-preset-expo",
            versionOrUri = "5.1.1",
            scope = NpmDependency.DevelopmentScope
        ),
        NpmDependency(
            name = "react-test-renderer",
            versionOrUri = "16.6.0-alpha.8af6728",
            scope = NpmDependency.DevelopmentScope
        )
    )

    @get:Input
    val moduleNames: List<String> by lazy {
        project.tasks.withType(KotlinJsCompile::class.java)
            .filter { !it.name.contains("test", ignoreCase = true) }
            .mapNotNull {
                it.kotlinOptions.outputFile?.substringAfterLast('/')?.substringAfterLast('\\')?.removeSuffix(".js")
            }
    }

    @TaskAction
    fun generate() {
        val packagesJson: Map<*, *> = mapOf(
            "name" to (moduleNames.singleOrNull() ?: project.name ?: "noname"),
            "version" to (toSemver(project.version.toString())),
            "description" to "simple description",
            "main" to "App.js",
            "dependencies" to (defaultDependencies.filter { it.scope == NpmDependency.RuntimeScope } + npm.dependencies).associateBy(
                { it.name },
                { it.versionOrUri }),
            "devDependencies" to (defaultDependencies.filter { it.scope == NpmDependency.DevelopmentScope } + npm.developmentDependencies).associateBy(
                { it.name },
                { it.versionOrUri })
        )

        packageJsonFile.writeText(JsonBuilder(packagesJson).toPrettyString())
        npmrcFile.writeText(
            """
        progress=false
        package-lock=false
        # cache-min=3600
        """.trimIndent()
        )
    }
}