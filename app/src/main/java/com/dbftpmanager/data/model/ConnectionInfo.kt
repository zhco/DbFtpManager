package com.dbftpmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * 连接信息实体
 */
@Entity(tableName = "connections")
data class ConnectionInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: ConnectionType,
    val host: String,
    val port: Int,
    val username: String = "",
    val password: String = "",
    val databaseName: String = "",
    val sshHost: String = "",
    val sshPort: Int = 22,
    val sshUsername: String = "",
    val sshPassword: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = 0L,
    val color: String = "#1976D2",
    val sortOrder: Int = 0
) : Serializable {

    val isDatabaseType: Boolean
        get() = type in listOf(
            ConnectionType.MYSQL, ConnectionType.POSTGRESQL,
            ConnectionType.SQLSERVER, ConnectionType.SQLITE,
            ConnectionType.ORACLE, ConnectionType.MARIADB
        )

    val isFtpType: Boolean
        get() = type == ConnectionType.FTP || type == ConnectionType.SFTP

    val displayTitle: String
        get() = when {
            isDatabaseType -> "$name ($databaseName)"
            else -> name
        }

    val displaySubtitle: String
        get() = when (type) {
            ConnectionType.SQLITE -> databaseName
            else -> "$host:$port"
        }
}

enum class ConnectionType(val displayName: String, val defaultPort: Int) {
    MYSQL("MySQL", 3306),
    POSTGRESQL("PostgreSQL", 5432),
    SQLSERVER("SQL Server", 1433),
    SQLITE("SQLite", 0),
    ORACLE("Oracle", 1521),
    MARIADB("MariaDB", 3306),
    FTP("FTP", 21),
    SFTP("SFTP", 22)
}
