package com.flutterforge.core.common

import com.flutterforge.domain.model.FlutterProjectInfo
import com.flutterforge.domain.model.PubspecDependency

object PubspecParser {

    fun parse(rawYaml: String): FlutterProjectInfo {
        val lines = rawYaml.lines()
        var name = ""
        var description: String? = null
        var version: String? = null
        var sdkConstraint: String? = null
        val dependencies = mutableListOf<PubspecDependency>()
        val devDependencies = mutableListOf<PubspecDependency>()
        var usesFlutter = false

        var currentSection: Section? = null
        var currentDepName: String? = null
        var inEnvironment = false

        for (rawLine in lines) {
            val trimmedRight = rawLine.trimEnd()
            if (trimmedRight.isBlank() || trimmedRight.trimStart().startsWith("#")) continue

            val indent = rawLine.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
            val content = stripInlineComment(trimmedRight.trim())
            if (content.isEmpty()) continue

            if (indent == 0) {
                inEnvironment = false
                currentSection = null
                currentDepName = null

                val colonIndex = content.indexOf(':')
                if (colonIndex != -1) {
                    val key = content.substring(0, colonIndex).trim()
                    val value = unquote(content.substring(colonIndex + 1).trim())
                    when (key) {
                        "name" -> name = value
                        "description" -> if (value.isNotEmpty()) description = value
                        "version" -> if (value.isNotEmpty()) version = value
                        "environment" -> inEnvironment = true
                        "dependencies" -> currentSection = Section.DEPENDENCIES
                        "dev_dependencies" -> currentSection = Section.DEV_DEPENDENCIES
                        "flutter" -> usesFlutter = true
                    }
                }
                continue
            }

            if (inEnvironment && indent in 1..4) {
                val colonIndex = content.indexOf(':')
                if (colonIndex != -1) {
                    val key = content.substring(0, colonIndex).trim()
                    val value = unquote(content.substring(colonIndex + 1).trim())
                    if (key == "sdk" && value.isNotEmpty()) {
                        sdkConstraint = value
                    }
                }
                continue
            }

            if (currentSection != null) {
                if (indent in 1..3) {
                    val colonIndex = content.indexOf(':')
                    if (colonIndex != -1) {
                        val depKey = content.substring(0, colonIndex).trim()
                        val depVal = unquote(content.substring(colonIndex + 1).trim())
                        if (depKey == "flutter") usesFlutter = true
                        currentDepName = depKey
                        val constraint = depVal.ifEmpty { "config" }
                        val item = PubspecDependency(name = depKey, version = constraint)
                        when (currentSection) {
                            Section.DEPENDENCIES -> dependencies.add(item)
                            Section.DEV_DEPENDENCIES -> devDependencies.add(item)
                        }
                    }
                } else if (indent >= 4 && currentDepName != null) {
                    val colonIndex = content.indexOf(':')
                    if (colonIndex != -1) {
                        val subKey = content.substring(0, colonIndex).trim()
                        val subVal = unquote(content.substring(colonIndex + 1).trim())
                        if (subKey == "sdk" && subVal == "flutter") {
                            usesFlutter = true
                            updateLastDependencyVersion(
                                list = if (currentSection == Section.DEPENDENCIES) dependencies else devDependencies,
                                depName = currentDepName,
                                newVersion = "sdk: flutter"
                            )
                        } else if (subKey == "git" || subKey == "path") {
                            updateLastDependencyVersion(
                                list = if (currentSection == Section.DEPENDENCIES) dependencies else devDependencies,
                                depName = currentDepName,
                                newVersion = if (subVal.isNotEmpty()) "$subKey: $subVal" else subKey
                            )
                        }
                    }
                }
            }
        }

        return FlutterProjectInfo(
            name = name.ifBlank { "flutter_project" },
            description = description,
            version = version,
            sdkConstraint = sdkConstraint,
            dependencies = dependencies,
            devDependencies = devDependencies,
            usesFlutter = usesFlutter,
            rawYaml = rawYaml
        )
    }

    private fun updateLastDependencyVersion(
        list: MutableList<PubspecDependency>,
        depName: String,
        newVersion: String
    ) {
        val index = list.indexOfLast { it.name == depName }
        if (index != -1 && list[index].version == "config") {
            list[index] = list[index].copy(version = newVersion)
        }
    }

    private fun stripInlineComment(line: String): String {
        var inSingleQuote = false
        var inDoubleQuote = false
        for (i in line.indices) {
            when (line[i]) {
                '\'' -> if (!inDoubleQuote) inSingleQuote = !inSingleQuote
                '"' -> if (!inSingleQuote) inDoubleQuote = !inDoubleQuote
                '#' -> if (!inSingleQuote && !inDoubleQuote) {
                    return line.substring(0, i).trim()
                }
            }
        }
        return line
    }

    private fun unquote(value: String): String {
        if (value.length >= 2) {
            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))
            ) {
                return value.substring(1, value.length - 1)
            }
        }
        return value
    }

    private enum class Section {
        DEPENDENCIES,
        DEV_DEPENDENCIES
    }
}
