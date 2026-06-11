package com.dbftpmanager.data.model

/**
 * 数据库表信息
 */
data class TableInfo(
    val name: String,
    val schema: String = "",
    val type: String = "TABLE",
    val rowCount: Long = -1,
    val comment: String = ""
)

/**
 * 列信息
 */
data class ColumnInfo(
    val name: String,
    val dataType: String,
    val isNullable: Boolean = true,
    val isPrimaryKey: Boolean = false,
    val defaultValue: String? = null,
    val comment: String = "",
    val characterMaximumLength: Int? = null,
    val numericPrecision: Int? = null,
    val numericScale: Int? = null
)

/**
 * 查询结果
 */
data class QueryResult(
    val columns: List<String> = emptyList(),
    val rows: List<List<Any?>> = emptyList(),
    val affectedRows: Int = -1,
    val executionTimeMs: Long = 0L,
    val error: String? = null,
    val isSelect: Boolean = true
) {
    val isSuccess: Boolean get() = error == null
    val rowCount: Int get() = rows.size
}
