package com.kgmyshin.kotlin.expo.extensions

import com.kgmyshin.kotlin.expo.utils.workspace
import groovy.lang.GroovyObjectSupport
import org.gradle.api.Project
import org.gradle.api.tasks.OutputDirectory
import java.io.File

open class KotlinExpoExtension(project: Project) : GroovyObjectSupport() {

    @OutputDirectory
    var outputDirectory: File? = project.workspace()
}