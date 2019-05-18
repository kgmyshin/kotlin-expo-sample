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

package com.kgmyshin.kotlin.expo.extensions

import groovy.lang.GroovyObjectSupport
import org.gradle.api.Project

open class NpmExtension(project: Project) : GroovyObjectSupport() {
    val dependencies: MutableList<NpmDependency> = ArrayList()

    val versionReplacements: MutableList<NpmDependency> = ArrayList()

    val developmentDependencies: MutableList<NpmDependency> = ArrayList()

    @JvmOverloads
    fun dependency(name: String, version: String = "*") {
        dependencies.add(
            NpmDependency(
                name,
                version,
                NpmDependency.RuntimeScope
            )
        )
    }

    fun replaceVersion(name: String, version: String) {
        versionReplacements.add(
            NpmDependency(
                name,
                version,
                NpmDependency.RuntimeScope
            )
        )
    }

    @JvmOverloads
    fun devDependency(name: String, version: String = "*") {
        developmentDependencies.add(
            NpmDependency(
                name,
                version,
                NpmDependency.DevelopmentScope
            )
        )
    }
}