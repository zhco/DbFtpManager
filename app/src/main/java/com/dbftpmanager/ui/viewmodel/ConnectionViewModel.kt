package com.dbftpmanager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dbftpmanager.App
import com.dbftpmanager.data.model.ConnectionInfo
import com.dbftpmanager.data.model.ConnectionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as App).connectionRepository

    private val _allConnections = MutableStateFlow<List<ConnectionInfo>>(emptyList())
    val allConnections: StateFlow<List<ConnectionInfo>> = _allConnections.asStateFlow()

    private val _dbConnections = MutableStateFlow<List<ConnectionInfo>>(emptyList())
    val dbConnections: StateFlow<List<ConnectionInfo>> = _dbConnections.asStateFlow()

    private val _ftpConnections = MutableStateFlow<List<ConnectionInfo>>(emptyList())
    val ftpConnections: StateFlow<List<ConnectionInfo>> = _ftpConnections.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadConnections()
    }

    private fun loadConnections() {
        viewModelScope.launch {
            repository.allConnections.collect { connections ->
                _allConnections.value = connections
                _dbConnections.value = connections.filter { it.isDatabaseType }
                _ftpConnections.value = connections.filter { it.isFtpType }
            }
        }
    }

    fun saveConnection(connection: ConnectionInfo, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.saveConnection(connection)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteConnection(connection: ConnectionInfo, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteConnection(connection)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun updateLastUsed(id: Long) {
        viewModelScope.launch {
            repository.updateLastUsed(id)
        }
    }
}
