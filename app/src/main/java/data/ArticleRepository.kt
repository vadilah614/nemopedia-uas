package com.example.nemopedia.data

import android.content.Context
import com.example.nemopedia.model.Article
import com.example.nemopedia.utils.PreferenceHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArticleRepository private constructor(
    private val articleDao: ArticleDao,
    private val preferenceHelper: PreferenceHelper
) {

    // ========================================
    // FLOW - REACTIVE DATA STREAMS
    // ========================================

    /**
     * Flow untuk observe semua artikel
     * UI akan otomatis update saat data berubah
     */
    val allArticles: Flow<List<Article>> = articleDao.getAllArticles()

    /**
     * Flow untuk observe bookmarked articles
     */
    val bookmarkedArticles: Flow<List<Article>> = articleDao.getBookmarkedArticles()


    // ========================================
    // CRUD OPERATIONS (SUSPEND FUNCTIONS)
    // ========================================

    /**
     * Get artikel by ID (suspend function)
     * @param articleId ID artikel
     * @return Article atau null jika tidak ditemukan
     */
    suspend fun getArticleById(articleId: Int): Article? {
        return articleDao.getArticleById(articleId)
    }

    /**
     * Insert artikel baru ke database (suspend)
     * @param article Artikel yang akan ditambahkan
     */
    suspend fun insertArticle(article: Article) {
        articleDao.insertArticle(article)
    }

    /**
     * Update artikel existing (suspend)
     * VALIDASI: Hanya artikel user-created yang bisa diupdate
     * @param article Artikel yang sudah diupdate
     * @return true jika berhasil, false jika gagal (bukan user-created)
     */
    suspend fun updateArticle(article: Article): Boolean {
        // Validasi: hanya user-created yang bisa diupdate
        val existingArticle = articleDao.getArticleById(article.id)
        return if (existingArticle != null && existingArticle.isUserCreated) {
            articleDao.updateArticle(article)
            true
        } else {
            false
        }
    }

    /**
     * Delete artikel by ID (suspend)
     * VALIDASI: Hanya artikel user-created yang bisa dihapus
     * @param articleId ID artikel yang akan dihapus
     * @return true jika berhasil, false jika gagal (bukan user-created)
     */
    suspend fun deleteArticle(articleId: Int): Boolean {
        // Validasi: hanya user-created yang bisa dihapus
        val article = articleDao.getArticleById(articleId)
        return if (article != null && article.isUserCreated) {
            articleDao.deleteArticleById(articleId)
            true
        } else {
            false
        }
    }

    /**
     * Toggle status bookmark artikel (suspend)
     * @param articleId ID artikel
     */
    suspend fun toggleBookmark(articleId: Int) {
        val article = articleDao.getArticleById(articleId)
        article?.let {
            articleDao.updateBookmark(articleId, !it.isBookmarked)
        }
    }


    // ========================================
    // SEARCH & FILTER (RETURN FLOW)
    // ========================================

    /**
     * Search articles by query (Flow)
     * Search di title, summary, dan content
     * @param query Search query
     * @return Flow<List<Article>>
     */
    fun searchArticles(query: String): Flow<List<Article>> {
        return if (query.isBlank()) {
            allArticles
        } else {
            articleDao.searchArticles(query)
        }
    }

    /**
     * Filter articles by category (Flow)
     * @param category Nama kategori ("Semua" untuk semua artikel)
     * @return Flow<List<Article>>
     */
    fun getArticlesByCategory(category: String): Flow<List<Article>> {
        return if (category == "Semua") {
            allArticles
        } else {
            articleDao.getArticlesByCategory(category)
        }
    }


    // ========================================
    // SORTING (RETURN FLOW)
    // ========================================

    /**
     * Sort type enum
     */
    enum class SortType {
        TITLE_ASC,      // A-Z
        TITLE_DESC,     // Z-A
        READ_TIME_ASC,  // Tercepat
        READ_TIME_DESC, // Terlama
        NEWEST,         // Terbaru (by ID)
        OLDEST          // Terlama (by ID)
    }

    /**
     * Get sorted articles (Flow)
     * @param sortType Tipe sorting
     * @return Flow<List<Article>> yang sudah disort
     */
    fun getSortedArticles(sortType: SortType): Flow<List<Article>> {
        return allArticles.map { articles ->
            when (sortType) {
                SortType.TITLE_ASC -> articles.sortedBy { it.title }
                SortType.TITLE_DESC -> articles.sortedByDescending { it.title }
                SortType.READ_TIME_ASC -> articles.sortedBy { it.readTimeMinutes }
                SortType.READ_TIME_DESC -> articles.sortedByDescending { it.readTimeMinutes }
                SortType.NEWEST -> articles.sortedByDescending { it.id }
                SortType.OLDEST -> articles.sortedBy { it.id }
            }
        }
    }


    // ========================================
    // STATISTICS
    // ========================================

    /**
     * Get total article count (suspend)
     * @return Total jumlah artikel
     */
    suspend fun getArticleCount(): Int {
        return articleDao.getArticleCount()
    }

    /**
     * Get article count by category (suspend)
     * @param category Nama kategori ("Semua" untuk total semua)
     * @return Jumlah artikel di kategori tersebut
     */
    suspend fun getArticleCountByCategory(category: String): Int {
        return if (category == "Semua") {
            articleDao.getArticleCount()
        } else {
            articleDao.getArticleCountByCategory(category)
        }
    }


    // ========================================
    // RECENTLY VIEWED
    // ========================================

    /**
     * Get recently viewed articles (Flow)
     * Maksimal 5 artikel terakhir yang dilihat
     * @return Flow<List<Article>>
     */
    fun getRecentlyViewedArticles(): Flow<List<Article>> {
        val recentIds = preferenceHelper.getRecentlyViewed()
        return allArticles.map { articles ->
            recentIds.mapNotNull { id ->
                articles.find { it.id == id }
            }
        }
    }

    /**
     * Add article to recently viewed
     * @param articleId ID artikel yang dibuka
     */
    fun addToRecentlyViewed(articleId: Int) {
        preferenceHelper.addRecentlyViewed(articleId)
    }


    // ========================================
    // CATEGORIES (FIXED)
    // ========================================

    /**
     * Get all categories (FIXED - tidak dinamis)
     * Include "Semua" di awal
     * @return List kategori: ["Semua", "Biologi", "Geografi", ...]
     */
    fun getAllCategories(): List<String> {
        val categories = preferenceHelper.getCategories()
        return listOf("Semua") + categories.sorted()
    }

    /**
     * Get categories for spinner (tanpa "Semua")
     * Untuk digunakan di AddArticleActivity dan EditArticleActivity
     * @return List kategori: ["Biologi", "Geografi", ...]
     */
    fun getCategoriesForSpinner(): List<String> {
        return preferenceHelper.getCategories().sorted()
    }


    // ========================================
    // SINGLETON PATTERN
    // ========================================

    companion object {
        @Volatile
        private var INSTANCE: ArticleRepository? = null

        /**
         * Get singleton instance of ArticleRepository
         * Thread-safe dengan synchronized
         * @param context Application context
         * @return ArticleRepository instance
         */
        fun getInstance(context: Context): ArticleRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getDatabase(context)
                val preferenceHelper = PreferenceHelper(context)
                val instance = ArticleRepository(database.articleDao(), preferenceHelper)
                INSTANCE = instance
                instance
            }
        }
    }
}