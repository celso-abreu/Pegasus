package com.flutterforge.domain.model

data class PubspecDependency(
    val name: String,
    val version: String
)

data class FlutterProjectInfo(
    val name: String,
    val description: String?,
    val version: String?,
    val sdkConstraint: String?,
    val dependencies: List<PubspecDependency>,
    val devDependencies: List<PubspecDependency>,
    val usesFlutter: Boolean,
    val rawYaml: String
)
