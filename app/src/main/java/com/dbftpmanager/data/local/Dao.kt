package com.dbftpmanager.data.local

import androidx.room.*
import com.dbftpmanager.data.model.ConnectionInfo
import com.dbftpmanager.data.model.QueryHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {

    @Query("SELECT * FROM connections ORDER BY sortOrder ASC, updatedAt DESC")
    fun getAllConnections(): Flow<List<ConnectionInfo>>

    @Query("SELECT * FROM connections ORDER BY sortOrder ASC, updatedAt DESC")
    suspend fun getAllConnectionsOnce(): List<ConnectionInfo>

    @Query("SELECT * FROM connections WHERE id = :id")
    suspend fun getConnectionById(id: Long): ConnectionInfo?

    @Query("SELECT * FROM connections WHERE type IN (:types) ORDER BY sortOrder ASC, updatedAt DESC")
    fun getConnectionsByTypes(types: List<String>): Flow<List<ConnectionInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: ConnectionInfo): Long

    @Update
    suspend fun updateConnection(connection: ConnectionInfo)

    @Delete
    suspend fun deleteConnection(connection: ConnectionInfo)

    @Query("DELETE FROM connections WHERE id = :id")
    suspend fun deleteConnectionById(id: Long)

    @Query("UPDATE connections SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface QueryHistoryDao {

    @Query("SELECT * FROM query_history WHERE connectionId = :connectionId ORDER BY createdAt DESC LIMIT :limit")
    fun getHistoryByConnection(connectionId: Long, limit: Int = 100): Flow<List<QueryHistory>>

    @Query("SELECT * FROM query_history WHERE connectionId = :connectionId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getHistoryByConnectionOnce(connectionId: Long, limit: Int = 100): List<QueryHistory>

    @Insert
    suspend fun insertHistory(history: QueryHistory)

    @Query("DELETE FROM query_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM query_history WHERE connectionId = :connectionId")
    suspend fun clearHistory(connectionId: Long)

    @Query("DELETE FROM query_history")
    suspend fun clearAllHistory()
}
