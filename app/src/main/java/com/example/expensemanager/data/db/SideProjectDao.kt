package com.example.expensemanager.data.db

import androidx.room.*
import com.example.expensemanager.data.model.SideProject
import kotlinx.coroutines.flow.Flow

@Dao
interface SideProjectDao {

    @Query("SELECT * FROM side_projects WHERE is_active = 1 ORDER BY id DESC")
    fun getAllSideProjects(): Flow<List<SideProject>>

    @Query("SELECT * FROM side_projects WHERE id = :id")
    fun getSideProjectById(id: Long): Flow<SideProject?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSideProject(project: SideProject): Long

    @Update
    suspend fun updateSideProject(project: SideProject)

    @Delete
    suspend fun deleteSideProject(project: SideProject)
}
