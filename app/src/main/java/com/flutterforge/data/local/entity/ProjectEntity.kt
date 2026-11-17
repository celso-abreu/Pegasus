package com.flutterforge.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "imported_projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
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
