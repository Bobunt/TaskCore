package com.example.taskcore.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.taskcore.data.tables.Users

@Dao
interface UsersDao {

    @Query("SELECT * FROM users WHERE login = :login LIMIT 1")
    suspend fun getByLogin(login: String): Users?

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    suspend fun existsByLogin(email: String): Boolean

    @Query("SELECT * FROM users ORDER BY login ASC")
    suspend fun getAll(): List<Users>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: Users): Long
}