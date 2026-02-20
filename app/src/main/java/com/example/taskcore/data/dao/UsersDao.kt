package com.example.taskcore.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.taskcore.data.tables.Users

@Dao
interface UsersDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: Users): Long

    @Update
    suspend fun update(user: Users): Int

    @Delete
    suspend fun delete(user: Users): Int

    @Query("SELECT * FROM users ORDER BY created_at_timestamp DESC")
    suspend fun getAll(): List<Users>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Users?

    @Query("SELECT * FROM users WHERE login = :login LIMIT 1")
    suspend fun getByLogin(login: String): Users?

    @Query("DELETE FROM users")
    suspend fun deleteAll(): Int
}