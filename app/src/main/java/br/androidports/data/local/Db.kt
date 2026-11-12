package br.androidports.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import br.androidports.domain.NewsItem
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "news")
data class NewsEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val imageUrl: String,
    val category: String,
    val sourceName: String,
    val sourceUrl: String,
    val publishedAt: Long,
    val likes: Int,
    val saved: Boolean = false,
    val liked: Boolean = false,
    val cachedAt: Long = 0L,
)

data class NewsFlag(
    val id: String,
    val saved: Boolean,
    val liked: Boolean,
)

@Dao
interface NewsDao {
    @Query("SELECT * FROM news ORDER BY publishedAt DESC")
    suspend fun all(): List<NewsEntity>

    @Query("SELECT * FROM news ORDER BY publishedAt DESC")
    fun observeAll(): Flow<List<NewsEntity>>

    @Query("SELECT * FROM news WHERE saved = 1 ORDER BY publishedAt DESC")
    fun observeSaved(): Flow<List<NewsEntity>>

    @Query("SELECT id, saved, liked FROM news")
    suspend fun flags(): List<NewsFlag>

    @Query("SELECT * FROM news WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): NewsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<NewsEntity>)

    @Query("UPDATE news SET saved = :saved WHERE id = :id")
    suspend fun setSaved(id: String, saved: Boolean)

    @Query("UPDATE news SET liked = :liked WHERE id = :id")
    suspend fun setLiked(id: String, liked: Boolean)

    @Query("DELETE FROM news WHERE saved = 0")
    suspend fun clearUnsaved()

    @Query("SELECT COUNT(*) FROM news")
    suspend fun count(): Int
}

fun NewsItem.toEntity(
    saved: Boolean = this.saved,
    liked: Boolean = this.liked,
    cachedAt: Long = System.currentTimeMillis(),
): NewsEntity = NewsEntity(
    id = id,
    title = title,
    summary = summary,
    content = content,
    imageUrl = imageUrl,
    category = category,
    sourceName = sourceName,
    sourceUrl = sourceUrl,
    publishedAt = publishedAt,
    likes = likes,
    saved = saved,
    liked = liked,
    cachedAt = cachedAt,
)

fun NewsEntity.toDomain(): NewsItem = NewsItem(
    id = id,
    title = title,
    summary = summary,
    content = content,
    imageUrl = imageUrl,
    category = category,
    sourceName = sourceName,
    sourceUrl = sourceUrl,
    publishedAt = publishedAt,
    likes = likes,
    saved = saved,
    liked = liked,
)

@Database(entities = [NewsEntity::class], version = 1, exportSchema = false)
abstract class PortsDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
}
