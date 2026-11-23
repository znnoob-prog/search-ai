package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "search_queries")
data class SearchQueryEntity(
    @PrimaryKey
    val query: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isDeepThinking: Boolean = false
)

@Dao
interface SearchQueryDao {
    @Query("SELECT * FROM search_queries ORDER BY timestamp DESC LIMIT 20")
    fun getRecentQueries(): Flow<List<SearchQueryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(entity: SearchQueryEntity)

    @Query("DELETE FROM search_queries WHERE query = :query")
    suspend fun deleteQuery(query: String)

    @Query("DELETE FROM search_queries")
    suspend fun clearAllQueries()
}

@Database(entities = [SearchQueryEntity::class], version = 1, exportSchema = false)
abstract class SearchDatabase : RoomDatabase() {
    abstract fun searchQueryDao(): SearchQueryDao

    companion object {
        @Volatile
        private var INSTANCE: SearchDatabase? = null

        fun getDatabase(context: Context): SearchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SearchDatabase::class.java,
                    "search_local_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
