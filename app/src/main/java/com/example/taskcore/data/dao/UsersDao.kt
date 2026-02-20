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

    @Query("SELECT * FROM users WHERE login = :login LIMIT 1")
    suspend fun getByLogin(login: String): Users?

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE login = :login)")
    suspend fun existsByLogin(login: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: Users): Long
}