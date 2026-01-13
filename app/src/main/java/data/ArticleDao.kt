package com.example.nemopedia.data

import androidx.room.*
import com.example.nemopedia.model.Article
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    // Get all articles
    @Query("SELECT * FROM articles ORDER BY id DESC")
    fun getAllArticles(): Flow<List<Article>>

    // Get article by ID
    @Query("SELECT * FROM articles WHERE id = :articleId")
    suspend fun getArticleById(articleId: Int): Article?

    // Get articles by category
    @Query("SELECT * FROM articles WHERE category = :category ORDER BY id DESC")
    fun getArticlesByCategory(category: String): Flow<List<Article>>

    // Search articles
    @Query("SELECT * FROM articles WHERE title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchArticles(query: String): Flow<List<Article>>

    // Get bookmarked articles
    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY id DESC")
    fun getBookmarkedArticles(): Flow<List<Article>>

    // Insert article
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: Article)

    // Insert multiple articles
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<Article>)

    // Update article
    @Update
    suspend fun updateArticle(article: Article)

    // Delete article
    @Delete
    suspend fun deleteArticle(article: Article)

    // Delete article by ID
    @Query("DELETE FROM articles WHERE id = :articleId")
    suspend fun deleteArticleById(articleId: Int)

    // Toggle bookmark
    @Query("UPDATE articles SET isBookmarked = :isBookmarked WHERE id = :articleId")
    suspend fun updateBookmark(articleId: Int, isBookmarked: Boolean)

    // Get article count
    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getArticleCount(): Int

    // Get article count by category
    @Query("SELECT COUNT(*) FROM articles WHERE category = :category")
    suspend fun getArticleCountByCategory(category: String): Int

    // Check if database is empty
    @Query("SELECT COUNT(*) FROM articles")
    suspend fun isDatabaseEmpty(): Int
}