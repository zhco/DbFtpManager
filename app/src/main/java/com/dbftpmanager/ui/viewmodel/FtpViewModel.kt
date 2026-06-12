package com.dbftpmanager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dbftpmanager.data.model.ConnectionInfo
import com.dbftpmanager.data.model.FtpFileEntry
import com.dbftpmanager.util.FtpClientManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FtpViewModel(application: Application) : AndroidViewModel(application) {

    private val ftpManager = FtpClientManager()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<FtpFileEntry>>(emptyList())
    val files: StateFlow<List<FtpFileEntry>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _pathHistory = MutableStateFlow<List<String>>(mutableListOf("/"))
    val pathHistory: StateFlow<List<String>> = _pathHistory.asStateFlow()

    fun connect(connection: ConnectionInfo) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = withContext(Dispatchers.IO) {
                    ftpManager.connect(connection)
                }
                _isConnected.value = success
                if (success) {
                    _currentPath.value = ftpManager.currentDirectory()
                    listFiles()
                } else {
                    _error.value = "FTP 连接失败"
                }
            } catch (e: Exception) {
                _error.value = "连接失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun listFiles(path: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val targetPath = path ?: _currentPath.value
                if (path != null && path != _currentPath.value) {
                    val success = withContext(Dispatchers.IO) {
                        ftpManager.changeDirectory(path)
                    }
                    if (success) {
                        _currentPath.value = ftpManager.currentDirectory()
                    }
                }
                val fileList = withContext(Dispatchers.IO) {
                    ftpManager.listFiles(_currentPath.value)
                }
                _files.value = fileList
            } catch (e: Exception) {
                _error.value = "获取文件列表失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun navigateTo(path: String) {
        val history = _pathHistory.value.toMutableList()
        history.add(path)
        _pathHistory.value = history
        listFiles(path)
    }

    fun navigateUp() {
        val history = _pathHistory.value.toMutableList()
        if (history.size > 1) {
            history.removeAt(history.lastIndex)
            _pathHistory.value = history
            val parentPath = history.last()
            listFiles(parentPath)
        }
    }

    fun uploadFile(localPath: String, remoteFileName: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentDir = _currentPath.value
                val remotePath = if (currentDir.endsWith("/")) "$currentDir$remoteFileName" else "$currentDir/$remoteFileName"
                android.util.Log.d("FtpViewModel", "上传文件: local=$localPath, remote=$remotePath")
                val result = withContext(Dispatchers.IO) {
                    ftpManager.uploadFileWithDetails(localPath, remotePath)
                }
                if (result.success) {
                    _message.value = "上传成功"
                    listFiles()
                } else {
                    _error.value = "上传失败: ${result.error}"
                    android.util.Log.e("FtpViewModel", "上传失败: ${result.error}")
                }
            } catch (e: Exception) {
                _error.value = "上传失败: ${e.message}"
                android.util.Log.e("FtpViewModel", "上传异常", e)
            } finally {
                _isLoading.value = false
                onComplete?.invoke()
            }
        }
    }

    fun downloadFile(remotePath: String, localPath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    ftpManager.downloadFile(remotePath, localPath)
                }
                if (success) {
                    _message.value = "下载成功: $localPath"
                } else {
                    _error.value = "下载失败"
                }
            } catch (e: Exception) {
                _error.value = "下载失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFile(path: String, isDirectory: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    if (isDirectory) {
                        ftpManager.deleteDirectory(path)
                    } else {
                        ftpManager.deleteFile(path)
                    }
                }
                if (success) {
                    _message.value = "删除成功"
                    listFiles()
                } else {
                    _error.value = "删除失败"
                }
            } catch (e: Exception) {
                _error.value = "删除失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createFolder(folderName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val path = "${_currentPath.value}/$folderName"
                val success = withContext(Dispatchers.IO) {
                    ftpManager.createDirectory(path)
                }
                if (success) {
                    _message.value = "文件夹创建成功"
                    listFiles()
                } else {
                    _error.value = "创建文件夹失败"
                }
            } catch (e: Exception) {
                _error.value = "创建文件夹失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun renameFile(from: String, to: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    ftpManager.rename(from, to)
                }
                if (success) {
                    _message.value = "重命名成功"
                    listFiles()
                } else {
                    _error.value = "重命名失败"
                }
            } catch (e: Exception) {
                _error.value = "重命名失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        listFiles()
    }

    fun clearError() {
        _error.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    fun disconnect() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ftpManager.disconnect()
            }
            _isConnected.value = false
            _files.value = emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ftpManager.disconnect()
    }
}
