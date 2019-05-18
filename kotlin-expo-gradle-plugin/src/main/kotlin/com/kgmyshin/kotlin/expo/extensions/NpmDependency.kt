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

import org.gradle.api.tasks.Input
import java.io.Serializable

data class NpmDependency(
    @Input val name: String,
    @Input val versionOrUri: String,
    @Input val scope: String
) : Serializable {
    companion object {
        val DevelopmentScope = "development"
        val RuntimeScope = "runtime"
    }
}