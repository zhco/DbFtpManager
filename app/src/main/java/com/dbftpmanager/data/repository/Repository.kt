package com.dbftpmanager.data.repository

import com.dbftpmanager.data.local.ConnectionDao
import com.dbftpmanager.data.local.QueryHistoryDao
import com.dbftpmanager.data.model.ConnectionInfo
import com.dbftpmanager.data.model.ConnectionType
import com.dbftpmanager.data.model.QueryHistory
import kotlinx.coroutines.flow.Flow

class ConnectionRepository(private val connectionDao: ConnectionDao) {

    val allConnections: Flow<List<ConnectionInfo>> = connectionDao.getAllConnections()

    fun getConnectionsByTypes(types: List<ConnectionType>): Flow<List<ConnectionInfo>> {
        return connectionDao.getConnectionsByTypes(types.map { it.name })
    }

    suspend fun getConnectionById(id: Long): ConnectionInfo? {
        return connectionDao.getConnectionById(id)
    }

    suspend fun saveConnection(connection: ConnectionInfo): Long {
        return connectionDao.insertConnection(connection)
    }

    suspend fun updateConnection(connection: ConnectionInfo) {
        connectionDao.updateConnection(connection)
    }

    suspend fun deleteConnection(connection: ConnectionInfo) {
        connectionDao.deleteConnection(connection)
    }

    suspend fun deleteConnectionById(id: Long) {
        connectionDao.deleteConnectionById(id)
    }

    suspend fun updateLastUsed(id: Long) {
        connectionDao.updateLastUsed(id)
    }
}

class QueryHistoryRepository(private val queryHistoryDao: QueryHistoryDao) {

    fun getHistoryByConnection(connectionId: Long): Flow<List<QueryHistory>> {
        return queryHistoryDao.getHistoryByConnection(connectionId)
    }

    suspend fun getHistoryOnce(connectionId: Long): List<QueryHistory> {
        return queryHistoryDao.getHistoryByConnectionOnce(connectionId)
    }

    suspend fun addHistory(history: QueryHistory) {
        queryHistoryDao.insertHistory(history)
    }

    suspend fun deleteHistory(id: Long) {
        queryHistoryDao.deleteHistory(id)
    }

    suspend fun clearHistory(connectionId: Long) {
        queryHistoryDao.clearHistory(connectionId)
    }

    suspend fun clearAllHistory() {
        queryHistoryDao.clearAllHistory()
    }
}
