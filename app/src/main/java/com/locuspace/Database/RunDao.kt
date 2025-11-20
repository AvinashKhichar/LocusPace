package com.locuspace.Database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {

    @Insert
    suspend fun insert(run: RunEntity)

    @Query("SELECT * FROM runs ORDER BY timestamp DESC")
    suspend fun getAllRunsList(): List<RunEntity>

    @Query("SELECT * FROM runs ORDER BY timestamp DESC")
    fun getAllRuns(): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRun(): RunEntity?

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertUser(user: User)
//
//    // Update full user (including username & photo)
//    @Update
//    suspend fun updateUser(user: User)
//
//    // Optional: get user (assuming single user row)
//    @Query("SELECT * FROM user LIMIT 1")
//    fun getUser(): Flow<User?>
//
//    // Optional: update only name + photo by id
//    @Query("UPDATE user SET username = :username, photoUri = :photoUri WHERE id = :id")
//    suspend fun updateUserNameAndPhoto(
//        id: Long,
//        username: String,
//        photoUri: String?
//    )
}