package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SavedText
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedTextDao {
    @Query("SELECT * FROM saved_texts ORDER BY orderIndex ASC, id ASC")
    fun getAllFlow(): Flow<List<SavedText>>

    @Query("SELECT * FROM saved_texts ORDER BY orderIndex ASC, id ASC")
    suspend fun getAll(): List<SavedText>

    @Query("SELECT * FROM saved_texts WHERE id = :id")
    suspend fun getById(id: Long): SavedText?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SavedText): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SavedText>)

    @Update
    suspend fun update(item: SavedText)

    @Update
    suspend fun updateAll(items: List<SavedText>)

    @Delete
    suspend fun delete(item: SavedText)

    @Query("DELETE FROM saved_texts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_texts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM saved_texts")
    suspend fun getCount(): Int
}
