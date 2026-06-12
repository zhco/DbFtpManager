package com.dbftpmanager.util

import android.util.Log
import com.dbftpmanager.data.model.*
import java.sql.*
import java.util.*

/**
 * 数据库管理器 - 支持多种数据库类型
 */
class DatabaseManager {

    private var connection: Connection? = null
    private var currentDatabase: String = ""

    init {
        // 注册 JDBC 驱动（Android 环境下需要手动加载）
        try {
            Class.forName("org.sqlite.JDBC")
        } catch (_: Exception) {}
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
        } catch (_: Exception) {}
        try {
            Class.forName("org.postgresql.Driver")
        } catch (_: Exception) {}
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
        } catch (_: Exception) {}
        try {
            Class.forName("oracle.jdbc.OracleDriver")
        } catch (_: Exception) {}
        try {
            Class.forName("org.mariadb.jdbc.Driver")
        } catch (_: Exception) {}
    }

    /**
     * 连接数据库
     */
    fun connect(
        type: ConnectionType,
        host: String,
        port: Int,
        username: String,
        password: String,
        database: String
    ): Boolean {
        return try {
            disconnect()
            val url = buildJdbcUrl(type, host, port, database)
            Log.d(TAG, "连接数据库: type=$type, url=$url")
            connection = DriverManager.getConnection(url, username, password)
            currentDatabase = database
            Log.d(TAG, "数据库连接成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "连接数据库失败: ${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }

    /**
     * 连接 SQLite（本地文件）
     */
    fun connectSqlite(filePath: String): Boolean {
        return try {
            disconnect()
            val url = "jdbc:sqlite:$filePath"
            connection = DriverManager.getConnection(url)
            currentDatabase = filePath
            true
        } catch (e: Exception) {
            Log.e(TAG, "连接 SQLite 失败", e)
            false
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        try {
            connection?.close()
        } catch (e: Exception) {
            Log.e(TAG, "断开连接失败", e)
        }
        connection = null
    }

    /**
     * 测试连接
     */
    fun testConnection(
        type: ConnectionType,
        host: String,
        port: Int,
        username: String,
        password: String,
        database: String
    ): Pair<Boolean, String> {
        return try {
            if (type == ConnectionType.SQLITE) {
                // SQLite 直接使用文件路径测试
                val conn = DriverManager.getConnection("jdbc:sqlite:$database")
                val valid = conn.isValid(5)
                conn.close()
                return Pair(valid, if (valid) "连接成功" else "连接验证失败")
            }
            val url = buildJdbcUrl(type, host, port, database)
            Log.d(TAG, "测试连接: type=$type, url=$url")
            val conn = DriverManager.getConnection(url, username, password)
            val valid = conn.isValid(5)
            conn.close()
            Pair(valid, if (valid) "连接成功" else "连接验证失败")
        } catch (e: Exception) {
            Log.e(TAG, "测试连接失败: ${e.javaClass.simpleName}: ${e.message}", e)
            Pair(false, "连接失败: ${e.message}")
        }
    }

    /**
     * 执行 SQL 查询
     */
    fun executeQuery(sql: String): QueryResult {
        val startTime = System.currentTimeMillis()
        return try {
            val conn = connection ?: return QueryResult(error = "未连接数据库")
            val trimmedSql = sql.trim().uppercase(Locale.getDefault())

            if (trimmedSql.startsWith("SELECT") || trimmedSql.startsWith("SHOW") ||
                trimmedSql.startsWith("DESCRIBE") || trimmedSql.startsWith("EXPLAIN") ||
                trimmedSql.startsWith("PRAGMA")
            ) {
                executeSelect(conn, sql, startTime)
            } else {
                executeUpdate(conn, sql, startTime)
            }
        } catch (e: Exception) {
            QueryResult(
                error = e.message ?: "未知错误",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun executeSelect(conn: Connection, sql: String, startTime: Long): QueryResult {
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                val metaData = rs.metaData
                val columns = (1..metaData.columnCount).map { metaData.getColumnName(it) }
                val rows = mutableListOf<List<Any?>>()

                while (rs.next()) {
                    val row = (1..columns.size).map { idx ->
                        rs.getObject(idx)
                    }
                    rows.add(row)
                }

                return QueryResult(
                    columns = columns,
                    rows = rows,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    isSelect = true
                )
            }
        }
    }

    private fun executeUpdate(conn: Connection, sql: String, startTime: Long): QueryResult {
        conn.createStatement().use { stmt ->
            val affected = stmt.executeUpdate(sql)
            return QueryResult(
                affectedRows = affected,
                executionTimeMs = System.currentTimeMillis() - startTime,
                isSelect = false
            )
        }
    }

    /**
     * 获取所有数据库列表
     */
    fun getDatabases(type: ConnectionType): List<String> {
        val conn = connection ?: return emptyList()
        return try {
            when (type) {
                ConnectionType.MYSQL, ConnectionType.MARIADB -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("SHOW DATABASES").use { rs ->
                            val list = mutableListOf<String>()
                            while (rs.next()) list.add(rs.getString(1))
                            list
                        }
                    }
                }
                ConnectionType.POSTGRESQL -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false").use { rs ->
                            val list = mutableListOf<String>()
                            while (rs.next()) list.add(rs.getString(1))
                            list
                        }
                    }
                }
                ConnectionType.SQLSERVER -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("SELECT name FROM sys.databases").use { rs ->
                            val list = mutableListOf<String>()
                            while (rs.next()) list.add(rs.getString(1))
                            list
                        }
                    }
                }
                ConnectionType.ORACLE -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("SELECT username FROM all_users ORDER BY username").use { rs ->
                            val list = mutableListOf<String>()
                            while (rs.next()) list.add(rs.getString(1))
                            list
                        }
                    }
                }
                ConnectionType.SQLITE -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'").use { rs ->
                            val list = mutableListOf<String>()
                            while (rs.next()) list.add(rs.getString(1))
                            list
                        }
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取数据库列表失败", e)
            emptyList()
        }
    }

    /**
     * 获取当前数据库的所有表
     */
    fun getTables(type: ConnectionType): List<TableInfo> {
        val conn = connection ?: return emptyList()
        return try {
            when (type) {
                ConnectionType.MYSQL, ConnectionType.MARIADB -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("SHOW TABLE STATUS").use { rs ->
                            val list = mutableListOf<TableInfo>()
                            while (rs.next()) {
                                list.add(
                                    TableInfo(
                                        name = rs.getString("Name"),
                                        type = rs.getString("Engine") ?: "TABLE",
                                        rowCount = rs.getLong("Rows"),
                                        comment = rs.getString("Comment") ?: ""
                                    )
                                )
                            }
                            list
                        }
                    }
                }
                ConnectionType.POSTGRESQL -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery(
                            "SELECT tablename, tableowner FROM pg_tables WHERE schemaname = 'public'"
                        ).use { rs ->
                            val list = mutableListOf<TableInfo>()
                            while (rs.next()) {
                                list.add(
                                    TableInfo(
                                        name = rs.getString("tablename"),
                                        schema = "public"
                                    )
                                )
                            }
                            list
                        }
                    }
                }
                ConnectionType.SQLSERVER -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery(
                            "SELECT TABLE_NAME, TABLE_SCHEMA FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'"
                        ).use { rs ->
                            val list = mutableListOf<TableInfo>()
                            while (rs.next()) {
                                list.add(
                                    TableInfo(
                                        name = rs.getString("TABLE_NAME"),
                                        schema = rs.getString("TABLE_SCHEMA")
                                    )
                                )
                            }
                            list
                        }
                    }
                }
                ConnectionType.ORACLE -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery(
                            "SELECT table_name FROM user_tables ORDER BY table_name"
                        ).use { rs ->
                            val list = mutableListOf<TableInfo>()
                            while (rs.next()) {
                                list.add(TableInfo(name = rs.getString(1)))
                            }
                            list
                        }
                    }
                }
                ConnectionType.SQLITE -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery(
                            "SELECT name, type FROM sqlite_master WHERE type IN ('table', 'view') ORDER BY name"
                        ).use { rs ->
                            val list = mutableListOf<TableInfo>()
                            while (rs.next()) {
                                list.add(
                                    TableInfo(
                                        name = rs.getString("name"),
                                        type = rs.getString("type")
                                    )
                                )
                            }
                            list
                        }
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取表列表失败", e)
            emptyList()
        }
    }

    /**
     * 获取表的列信息
     */
    fun getColumns(type: ConnectionType, tableName: String): List<ColumnInfo> {
        val conn = connection ?: return emptyList()
        return try {
            when (type) {
                ConnectionType.MYSQL, ConnectionType.MARIADB -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("DESCRIBE `$tableName`").use { rs ->
                            val list = mutableListOf<ColumnInfo>()
                            while (rs.next()) {
                                list.add(
                                    ColumnInfo(
                                        name = rs.getString("Field"),
                                        dataType = rs.getString("Type"),
                                        isNullable = rs.getString("Null") == "YES",
                                        isPrimaryKey = rs.getString("Key") == "PRI",
                                        defaultValue = rs.getString("Default"),
                                        comment = rs.getString("Extra") ?: ""
                                    )
                                )
                            }
                            list
                        }
                    }
                }
                ConnectionType.POSTGRESQL -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery(
                            """SELECT column_name, data_type, is_nullable, column_default,
                                character_maximum_length, numeric_precision, numeric_scale
                               FROM information_schema.columns
                               WHERE table_name = '$tableName' AND table_schema = 'public'
                               ORDER BY ordinal_position"""
                        ).use { rs ->
                            val list = mutableListOf<ColumnInfo>()
                            while (rs.next()) {
                                list.add(
                                    ColumnInfo(
                                        name = rs.getString("column_name"),
                                        dataType = rs.getString("data_type"),
                                        isNullable = rs.getString("is_nullable") == "YES",
                                        defaultValue = rs.getString("column_default"),
                                        characterMaximumLength = rs.getInt("character_maximum_length").takeIf { !rs.wasNull() },
                                        numericPrecision = rs.getInt("numeric_precision").takeIf { !rs.wasNull() },
                                        numericScale = rs.getInt("numeric_scale").takeIf { !rs.wasNull() }
                                    )
                                )
                            }
                            list
                        }
                    }
                }
                ConnectionType.SQLSERVER -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery(
                            """SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT,
                                CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE
                               FROM INFORMATION_SCHEMA.COLUMNS
                               WHERE TABLE_NAME = '$tableName'
                               ORDER BY ORDINAL_POSITION"""
                        ).use { rs ->
                            val list = mutableListOf<ColumnInfo>()
                            while (rs.next()) {
                                list.add(
                                    ColumnInfo(
                                        name = rs.getString("COLUMN_NAME"),
                                        dataType = rs.getString("DATA_TYPE"),
                                        isNullable = rs.getString("IS_NULLABLE") == "YES",
                                        defaultValue = rs.getString("COLUMN_DEFAULT"),
                                        characterMaximumLength = rs.getInt("CHARACTER_MAXIMUM_LENGTH").takeIf { !rs.wasNull() },
                                        numericPrecision = rs.getInt("NUMERIC_PRECISION").takeIf { !rs.wasNull() },
                                        numericScale = rs.getInt("NUMERIC_SCALE").takeIf { !rs.wasNull() }
                                    )
                                )
                            }
                            list
                        }
                    }
                }
                ConnectionType.SQLITE -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("PRAGMA table_info(`$tableName`)").use { rs ->
                            val list = mutableListOf<ColumnInfo>()
                            while (rs.next()) {
                                list.add(
                                    ColumnInfo(
                                        name = rs.getString("name"),
                                        dataType = rs.getString("type") ?: "",
                                        isPrimaryKey = rs.getInt("pk") > 0,
                                        defaultValue = rs.getString("dflt_value")
                                    )
                                )
                            }
                            list
                        }
                    }
                }
                ConnectionType.ORACLE -> {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery(
                            """SELECT column_name, data_type, nullable, data_default,
                                data_length, data_precision, data_scale
                               FROM user_tab_columns WHERE table_name = '${tableName.uppercase()}'
                               ORDER BY column_id"""
                        ).use { rs ->
                            val list = mutableListOf<ColumnInfo>()
                            while (rs.next()) {
                                list.add(
                                    ColumnInfo(
                                        name = rs.getString("column_name"),
                                        dataType = rs.getString("data_type"),
                                        isNullable = rs.getString("nullable") == "Y",
                                        defaultValue = rs.getString("data_default"),
                                        characterMaximumLength = rs.getInt("data_length").takeIf { !rs.wasNull() },
                                        numericPrecision = rs.getInt("data_precision").takeIf { !rs.wasNull() },
                                        numericScale = rs.getInt("data_scale").takeIf { !rs.wasNull() }
                                    )
                                )
                            }
                            list
                        }
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取列信息失败", e)
            emptyList()
        }
    }

    /**
     * 获取表数据（分页）
     */
    fun getTableData(tableName: String, offset: Int = 0, limit: Int = 500): QueryResult {
        val sql = "SELECT * FROM `$tableName` LIMIT $limit OFFSET $offset"
        return executeQuery(sql)
    }

    /**
     * 切换数据库
     */
    fun switchDatabase(type: ConnectionType, databaseName: String): Boolean {
        val conn = connection ?: return false
        return try {
            when (type) {
                ConnectionType.MYSQL, ConnectionType.MARIADB -> {
                    conn.createStatement().execute("USE `$databaseName`")
                    currentDatabase = databaseName
                    true
                }
                ConnectionType.POSTGRESQL -> {
                    conn.setCatalog(databaseName)
                    currentDatabase = databaseName
                    true
                }
                ConnectionType.SQLSERVER -> {
                    conn.createStatement().execute("USE `$databaseName`")
                    currentDatabase = databaseName
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "切换数据库失败", e)
            false
        }
    }

    /**
     * 构建 JDBC URL
     */
    private fun buildJdbcUrl(type: ConnectionType, host: String, port: Int, database: String): String {
        return when (type) {
            ConnectionType.MYSQL -> "jdbc:mysql://$host:$port/$database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8"
            ConnectionType.POSTGRESQL -> "jdbc:postgresql://$host:$port/$database?sslmode=disable"
            ConnectionType.SQLSERVER -> "jdbc:sqlserver://$host:$port;databaseName=$database;encrypt=false"
            ConnectionType.ORACLE -> "jdbc:oracle:thin:@$host:$port:$database"
            ConnectionType.MARIADB -> "jdbc:mariadb://$host:$port/$database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            ConnectionType.SQLITE -> throw UnsupportedOperationException("SQLite 请使用 connectSqlite 方法")
            else -> throw IllegalArgumentException("不支持的数据库类型: $type")
        }
    }

    val isConnected: Boolean get() = connection != null && connection?.isClosed == false

    companion object {
        private const val TAG = "DatabaseManager"
    }
}
