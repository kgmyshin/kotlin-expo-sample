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

import org.gradle.api.Project.DEFAULT_VERSION

fun toSemver(version: String?) = buildString {
    if (version == null ||
        version == DEFAULT_VERSION
    ) {

        return "0.0.0"
    }

    val numericStart = "^\\d+(\\.\\d+){0,2}".toRegex().find(version)?.value
    val numericParts = numericStart?.split(".")?.map(::dropLeadingZeroes).orEmpty().padEnd(3, "0")

    numericParts.joinTo(this, ".")

    val remainingString = version.substring(numericStart?.length ?: 0)
    if (remainingString.isNotBlank()) {
        val separators = "[._\\-+]+".toRegex().findAll(remainingString)
        val it = separators.iterator()
        var lastIndex = 0

        fun cutAndDropZeroes(end: Int) {
            val part = remainingString.substring(lastIndex, end)
            if (part.isNotEmpty() && part.all(Char::isDigit)) {
                append(dropLeadingZeroes(part))
            } else {
                append(part)
            }
        }

        var first = true
        while (it.hasNext()) {
            val separator = it.next()
            cutAndDropZeroes(separator.range.start)
            if (first) {
                append("-")
                first = false
            } else {
                append(replaceSeparator(separator.value))
            }
            lastIndex = separator.range.endInclusive + 1
        }

        if (lastIndex < remainingString.length) {
            cutAndDropZeroes(remainingString.length)
        }
    }
}

private fun <T> List<T>.padEnd(size: Int, value: T): List<T> {
    if (this.size >= size) {
        return this
    }

    val result = toMutableList()
    while (result.size < size) {
        result.add(value)
    }

    return result
}


private fun dropLeadingZeroes(s: String): String {
    val r = s.dropWhile { it == '0' }
    return if (r.isEmpty()) "0" else r
}

private fun replaceSeparator(s: String) = when {
    s == "." || s == "-" -> s
    s.all { it == '-' } -> "-"
    s.all { it == '.' } -> "."
    else -> "."
}