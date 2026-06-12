package com.dbftpmanager.util

import android.util.Log
import com.dbftpmanager.data.model.ConnectionInfo
import com.dbftpmanager.data.model.ConnectionType
import com.dbftpmanager.data.model.FtpFileEntry
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * FTP 客户端管理器
 */
class FtpClientManager {

    private var ftpClient: FTPClient? = null
    private var isConnected = false

    /**
     * 连接 FTP 服务器
     */
    fun connect(connectionInfo: ConnectionInfo): Boolean {
        return try {
            disconnect()
            val client = FTPClient()

            client.connect(connectionInfo.host, connectionInfo.port)
            val reply = client.replyCode

            if (!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect()
                return false
            }

            val loggedIn = client.login(
                connectionInfo.username,
                connectionInfo.password
            )

            if (!loggedIn) {
                client.disconnect()
                return false
            }

            // 设置传输模式
            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)
            client.setControlKeepAliveTimeout(60)
            client.setDataTimeout(30000)
            client.soTimeout = 30000

            ftpClient = client
            isConnected = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "FTP 连接失败", e)
            isConnected = false
            false
        }
    }

    /**
     * 测试连接
     */
    fun testConnection(connectionInfo: ConnectionInfo): Pair<Boolean, String> {
        return try {
            val client = FTPClient()
            client.connect(connectionInfo.host, connectionInfo.port)
            val reply = client.replyCode

            if (!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect()
                return Pair(false, "服务器拒绝连接 (回复码: $reply)")
            }

            val loggedIn = client.login(
                connectionInfo.username,
                connectionInfo.password
            )

            if (!loggedIn) {
                client.disconnect()
                return Pair(false, "登录失败，请检查用户名和密码")
            }

            client.logout()
            client.disconnect()
            Pair(true, "连接测试成功")
        } catch (e: Exception) {
            Pair(false, "连接失败: ${e.message}")
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        try {
            ftpClient?.logout()
            ftpClient?.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "断开 FTP 连接失败", e)
        }
        ftpClient = null
        isConnected = false
    }

    /**
     * 获取当前目录的文件列表
     */
    fun listFiles(path: String = ""): List<FtpFileEntry> {
        val client = ftpClient ?: return emptyList()
        return try {
            val targetPath = if (path.isEmpty()) client.printWorkingDirectory() else path
            val files = client.listFiles(targetPath) ?: return emptyList()
            files.filter { it.name != "." && it.name != ".." }.map { file ->
                FtpFileEntry(
                    name = file.name,
                    fullPath = if (targetPath.endsWith("/")) "$targetPath${file.name}" else "$targetPath/${file.name}",
                    isDirectory = file.isDirectory,
                    size = file.size,
                    lastModified = file.timestamp?.timeInMillis ?: 0L,
                    permissions = formatPermissions(file),
                    isHidden = file.name.startsWith(".")
                )
            }.sortedWith(compareByDescending<FtpFileEntry> { it.isDirectory }.thenBy { it.name })
        } catch (e: Exception) {
            Log.e(TAG, "获取文件列表失败", e)
            emptyList()
        }
    }

    /**
     * 切换目录
     */
    fun changeDirectory(path: String): Boolean {
        val client = ftpClient ?: return false
        return try {
            client.changeWorkingDirectory(path)
        } catch (e: Exception) {
            Log.e(TAG, "切换目录失败", e)
            false
        }
    }

    /**
     * 获取当前工作目录
     */
    fun currentDirectory(): String {
        val client = ftpClient ?: return ""
        return try {
            client.printWorkingDirectory() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 上传文件
     */
    fun uploadFile(localPath: String, remotePath: String): Boolean {
        val client = ftpClient ?: return false
        return try {
            val inputStream = java.io.FileInputStream(localPath)
            val success = client.storeFile(remotePath, inputStream)
            inputStream.close()
            success
        } catch (e: Exception) {
            Log.e(TAG, "上传文件失败", e)
            false
        }
    }

    /**
     * 上传文件（通过 InputStream）
     */
    fun uploadFile(inputStream: InputStream, remotePath: String): Boolean {
        val client = ftpClient ?: return false
        return try {
            client.storeFile(remotePath, inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "上传文件失败", e)
            false
        }
    }

    /**
     * 下载文件
     */
    fun downloadFile(remotePath: String, localPath: String): Boolean {
        val client = ftpClient ?: return false
        return try {
            val outputStream = java.io.FileOutputStream(localPath)
            val success = client.retrieveFile(remotePath, outputStream)
            outputStream.close()
            success
        } catch (e: Exception) {
            Log.e(TAG, "下载文件失败", e)
            false
        }
    }

    /**
     * 下载文件到 OutputStream
     */
    fun downloadFile(remotePath: String, outputStream: OutputStream): Boolean {
        val client = ftpClient ?: return false
        return try {
            client.retrieveFile(remotePath, outputStream)
        } catch (e: Exception) {
            Log.e(TAG, "下载文件失败", e)
            false
        }
    }

    /**
     * 删除文件
     */
    fun deleteFile(path: String): Boolean {
        val client = ftpClient ?: return false
        return try {
            client.deleteFile(path)
        } catch (e: Exception) {
            Log.e(TAG, "删除文件失败", e)
            false
        }
    }

    /**
     * 删除目录
     */
    fun deleteDirectory(path: String): Boolean {
        val client = ftpClient ?: return false
        return try {
            client.removeDirectory(path)
        } catch (e: Exception) {
            Log.e(TAG, "删除目录失败", e)
            false
        }
    }

    /**
     * 创建目录
     */
    fun createDirectory(path: String): Boolean {
        val client = ftpClient ?: return false
        return try {
            client.makeDirectory(path)
        } catch (e: Exception) {
            Log.e(TAG, "创建目录失败", e)
            false
        }
    }

    /**
     * 重命名文件/目录
     */
    fun rename(from: String, to: String): Boolean {
        val client = ftpClient ?: return false
        return try {
            client.rename(from, to)
        } catch (e: Exception) {
            Log.e(TAG, "重命名失败", e)
            false
        }
    }

    /**
     * 获取文件大小
     */
    fun getFileSize(path: String): Long {
        val client = ftpClient ?: return -1
        return try {
            client.getSize(path).toLongOrNull() ?: -1L
        } catch (e: Exception) {
            -1
        }
    }

    private fun formatPermissions(file: FTPFile): String {
        val sb = StringBuilder()
        sb.append(if (file.isDirectory) "d" else "-")
        sb.append(if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION)) "r" else "-")
        sb.append(if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION)) "w" else "-")
        sb.append(if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION)) "x" else "-")
        sb.append(if (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION)) "r" else "-")
        sb.append(if (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION)) "w" else "-")
        sb.append(if (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION)) "x" else "-")
        sb.append(if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION)) "r" else "-")
        sb.append(if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION)) "w" else "-")
        sb.append(if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION)) "x" else "-")
        return sb.toString()
    }

    companion object {
        private const val TAG = "FtpClientManager"
    }
}

// FTPReply 兼容
private object FTPReply {
    fun isPositiveCompletion(reply: Int): Boolean = reply in 200..299
}
