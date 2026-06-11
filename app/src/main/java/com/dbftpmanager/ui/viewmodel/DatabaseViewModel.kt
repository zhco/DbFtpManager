package com.dbftpmanager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dbftpmanager.App
import com.dbftpmanager.data.model.*
import com.dbftpmanager.util.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DatabaseViewModel(application: Application) : AndroidViewModel(application) {

    private val queryHistoryRepository = (application as App).queryHistoryRepository
    private val databaseManager = DatabaseManager()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentDatabase = MutableStateFlow("")
    val currentDatabase: StateFlow<String> = _currentDatabase.asStateFlow()

    private val _databases = MutableStateFlow<List<String>>(emptyList())
    val databases: StateFlow<List<String>> = _databases.asStateFlow()

    private val _tables = MutableStateFlow<List<TableInfo>>(emptyList())
    val tables: StateFlow<List<TableInfo>> = _tables.asStateFlow()

    private val _currentTable = MutableStateFlow("")
    val currentTable: StateFlow<String> = _currentTable.asStateFlow()

    private val _columns = MutableStateFlow<List<ColumnInfo>>(emptyList())
    val columns: StateFlow<List<ColumnInfo>> = _columns.asStateFlow()

    private val _queryResult = MutableStateFlow<QueryResult?>(null)
    val queryResult: StateFlow<QueryResult?> = _queryResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _sqlHistory = MutableStateFlow<List<QueryHistory>>(emptyList())
    val sqlHistory: StateFlow<List<QueryHistory>> = _sqlHistory.asStateFlow()

    private var currentConnectionId: Long = 0
    private var currentConnectionType: ConnectionType = ConnectionType.MYSQL

    fun connect(connection: ConnectionInfo) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = withContext(Dispatchers.IO) {
                    if (connection.type == ConnectionType.SQLITE) {
                        databaseManager.connectSqlite(connection.databaseName)
                    } else {
                        databaseManager.connect(
                            connection.type, connection.host, connection.port,
                            connection.username, connection.password, connection.databaseName
                        )
                    }
                }
                _isConnected.value = success
                if (success) {
                    currentConnectionId = connection.id
                    currentConnectionType = connection.type
                    _currentDatabase.value = connection.databaseName
                    loadDatabases()
                    loadTables()
                    loadHistory()
                } else {
                    _error.value = "连接失败"
                }
            } catch (e: Exception) {
                _error.value = "连接失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadDatabases() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    databaseManager.getDatabases(currentConnectionType)
                }
                _databases.value = list
            } catch (e: Exception) {
                _error.value = "获取数据库列表失败"
            }
        }
    }

    fun switchDatabase(databaseName: String) {
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    databaseManager.switchDatabase(currentConnectionType, databaseName)
                }
                if (success) {
                    _currentDatabase.value = databaseName
                    loadTables()
                }
            } catch (e: Exception) {
                _error.value = "切换数据库失败"
            }
        }
    }

    fun loadTables() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    databaseManager.getTables(currentConnectionType)
                }
                _tables.value = list
            } catch (e: Exception) {
                _error.value = "获取表列表失败"
            }
        }
    }

    fun selectTable(tableName: String) {
        viewModelScope.launch {
            _currentTable.value = tableName
            try {
                val cols = withContext(Dispatchers.IO) {
                    databaseManager.getColumns(currentConnectionType, tableName)
                }
                _columns.value = cols
                // 自动加载表数据
                executeQuery("SELECT * FROM `$tableName` LIMIT 500")
            } catch (e: Exception) {
                _error.value = "加载表信息失败"
            }
        }
    }

    fun executeQuery(sql: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    databaseManager.executeQuery(sql)
                }
                _queryResult.value = result
                // 保存历史
                if (currentConnectionId > 0) {
                    queryHistoryRepository.addHistory(
                        QueryHistory(
                            connectionId = currentConnectionId,
                            databaseName = _currentDatabase.value,
                            sql = sql,
                            executionTimeMs = result.executionTimeMs,
                            success = result.isSuccess,
                            errorMessage = result.error
                        )
                    )
                    loadHistory()
                }
            } catch (e: Exception) {
                _queryResult.value = QueryResult(error = e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            if (currentConnectionId > 0) {
                queryHistoryRepository.getHistoryByConnection(currentConnectionId).collect { list ->
                    _sqlHistory.value = list
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                databaseManager.disconnect()
            }
            _isConnected.value = false
            _tables.value = emptyList()
            _columns.value = emptyList()
            _queryResult.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        databaseManager.disconnect()
    }
}
