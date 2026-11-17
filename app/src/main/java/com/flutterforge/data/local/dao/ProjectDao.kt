package com.flutterforge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flutterforge.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM imported_projects ORDER BY importedAtMillis DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM imported_projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM imported_projects WHERE id = :id")
    suspend fun deleteById(id: String)
}
