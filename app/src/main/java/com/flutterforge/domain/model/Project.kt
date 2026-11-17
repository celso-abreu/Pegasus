package com.flutterforge.domain.model

data class Project(
    val id: String,
    val name: String,
    val owner: String,
    val repoName: String,
    val branch: String,
    val localPath: String,
    val sizeBytes: Long,
    val importedAtMillis: Long,
    val hasPubspec: Boolean,
    val hasMainDart: Boolean
)
