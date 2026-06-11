package com.dbftpmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SQL 查询历史记录
 */
@Entity(tableName = "query_history")
data class QueryHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val connectionId: Long,
    val databaseName: String,
    val sql: String,
    val executionTimeMs: Long = 0L,
    val success: Boolean = true,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
