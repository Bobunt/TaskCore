package com.example.taskcore.data.dao

import androidx.room.*
import com.example.taskcore.data.tables.Tasks

@Dao
interface TasksDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Tasks): Long

    @Update
    suspend fun update(task: Tasks): Int

    @Delete
    suspend fun delete(task: Tasks): Int

    @Query("SELECT * FROM tasks ORDER BY created_at_timestamp DESC")
    suspend fun getAll(): List<Tasks>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Tasks?

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY created_at_timestamp DESC")
    suspend fun getByStatus(status: String): List<Tasks>

    @Query("SELECT * FROM tasks WHERE assignee = :assignee ORDER BY due_date_timestamp ASC")
    suspend fun getByAssignee(assignee: String): List<Tasks>

    @Query("DELETE FROM tasks")
    suspend fun deleteAll(): Int
}