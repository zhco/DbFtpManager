package com.dbftpmanager.data.model

/**
 * FTP 文件条目
 */
data class FtpFileEntry(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val permissions: String = "",
    val isHidden: Boolean = false
) {
    val formattedSize: String
        get() = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024))
            else -> String.format("%.2f GB", size / (1024.0 * 1024 * 1024))
        }

    val fileExtension: String
        get() {
            val dotIndex = name.lastIndexOf('.')
            return if (dotIndex > 0) name.substring(dotIndex + 1).lowercase() else ""
        }
}
