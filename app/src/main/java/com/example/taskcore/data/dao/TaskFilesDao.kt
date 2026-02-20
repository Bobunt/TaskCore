package com.example.taskcore.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.taskcore.data.tables.TaskFiles

@Dao
interface TaskFilesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: TaskFiles): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<TaskFiles>): List<Long>

    @Delete
    suspend fun delete(file: TaskFiles): Int

    @Query("SELECT * FROM task_files WHERE task_id = :taskId ORDER BY created_at_timestamp DESC")
    suspend fun getByTaskId(taskId: Int): List<TaskFiles>

    @Query("SELECT * FROM task_files WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): TaskFiles?

    @Query("DELETE FROM task_files WHERE task_id = :taskId")
    suspend fun deleteByTaskId(taskId: Int): Int
}