package com.dbftpmanager.ui.database

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dbftpmanager.App
import com.dbftpmanager.R
import com.dbftpmanager.data.model.ColumnInfo
import com.dbftpmanager.data.model.ConnectionInfo
import kotlinx.coroutines.launch

class TableDataActivity : AppCompatActivity() {

    private lateinit var viewModel: DatabaseViewModel
    private lateinit var connection: ConnectionInfo
    private var tableName: String = ""
    private var showStructure = false

    private lateinit var columnAdapter: ColumnAdapter
    private lateinit var dataAdapter: TableDataAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_table_data)

        viewModel = DatabaseViewModel(application as App)

        connection = intent.getSerializableExtra("connection") as ConnectionInfo
        tableName = intent.getStringExtra("table_name") ?: ""

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.title = tableName
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerViewStructure = findViewById<RecyclerView>(R.id.recyclerViewStructure)
        val recyclerViewData = findViewById<RecyclerView>(R.id.recyclerViewData)
        val swipeRefresh = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        val chipStructure = findViewById<com.google.android.material.chip.Chip>(R.id.chipStructure)
        val chipData = findViewById<com.google.android.material.chip.Chip>(R.id.chipData)

        columnAdapter = ColumnAdapter()
        dataAdapter = TableDataAdapter()

        recyclerViewStructure.layoutManager = LinearLayoutManager(this)
        recyclerViewStructure.adapter = columnAdapter

        recyclerViewData.layoutManager = LinearLayoutManager(this)
        recyclerViewData.adapter = dataAdapter

        chipData.setOnClickListener {
            showStructure = false
            chipData.isChecked = true
            chipStructure.isChecked = false
            recyclerViewStructure.visibility = View.GONE
            swipeRefresh.visibility = View.VISIBLE
        }

        chipStructure.setOnClickListener {
            showStructure = true
            chipStructure.isChecked = true
            chipData.isChecked = false
            recyclerViewStructure.visibility = View.VISIBLE
            swipeRefresh.visibility = View.GONE
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.executeQuery("SELECT * FROM `$tableName` LIMIT 500")
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddRow)
            .setOnClickListener {
                showInsertDialog()
            }

        lifecycleScope.launch {
            viewModel.columns.collect { columns ->
                columnAdapter.submitList(columns)
            }
        }

        lifecycleScope.launch {
            viewModel.queryResult.collect { result ->
                swipeRefresh.isRefreshing = false
                if (result != null && result.isSuccess && result.isSelect) {
                    dataAdapter.submitData(result.columns, result.rows)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                // Handle loading state
            }
        }

        // 加载数据
        viewModel.selectTable(tableName)
    }

    private fun showInsertDialog() {
        val columns = viewModel.columns.value
        if (columns.isEmpty()) return

        val scrollView = android.widget.ScrollView(this)
        val linearLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val inputs = mutableListOf<android.widget.EditText>()
        for (col in columns) {
            val til = com.google.android.material.textfield.TextInputLayout(this).apply {
                hint = "${col.name} (${col.dataType})"
                isExpandedHintEnabled = true
            }
            val et = android.widget.EditText(this)
            til.addView(et)
            linearLayout.addView(til)
            inputs.add(et)
        }

        scrollView.addView(linearLayout)

        android.app.AlertDialog.Builder(this)
            .setTitle("插入新行 - $tableName")
            .setView(scrollView)
            .setPositiveButton("插入") { _, _ ->
                val values = inputs.mapIndexed { index, et ->
                    val col = columns[index]
                    val value = et.text.toString().trim()
                    if (col.dataType.contains("int", ignoreCase = true) ||
                        col.dataType.contains("decimal", ignoreCase = true) ||
                        col.dataType.contains("float", ignoreCase = true) ||
                        col.dataType.contains("double", ignoreCase = true) ||
                        col.dataType.contains("numeric", ignoreCase = true)
                    ) {
                        value.ifEmpty { "NULL" }
                    } else {
                        if (value.isEmpty()) "NULL" else "'$value'"
                    }
                }
                val sql = "INSERT INTO `$tableName` (${columns.joinToString(",") { "`$it`" }}) VALUES (${values.joinToString(",")})"
                viewModel.executeQuery(sql)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    companion object {
        fun newIntent(context: Context, connection: ConnectionInfo, tableName: String) =
            Intent(context, TableDataActivity::class.java).apply {
                putExtra("connection", connection)
                putExtra("table_name", tableName)
            }
    }
}

class ColumnAdapter : RecyclerView.Adapter<ColumnAdapter.ViewHolder>() {

    private var columns = listOf<ColumnInfo>()

    fun submitList(list: List<ColumnInfo>) {
        columns = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_column, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(columns[position])
    }

    override fun getItemCount() = columns.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvColumnName)
        private val tvType: TextView = view.findViewById(R.id.tvColumnType)
        private val tvNullable: TextView = view.findViewById(R.id.tvColumnNullable)
        private val tvKey: TextView = view.findViewById(R.id.tvColumnKey)

        fun bind(column: ColumnInfo) {
            tvName.text = column.name
            tvType.text = column.dataType
            tvNullable.text = if (column.isNullable) "NULL" else "NOT NULL"
            tvKey.text = if (column.isPrimaryKey) "PK" else ""
        }
    }
}

class TableDataAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var columns = listOf<String>()
    private var rows = listOf<List<Any?>>()

    fun submitData(cols: List<String>, data: List<List<Any?>>) {
        columns = cols
        rows = data
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = rows[position]
        val text = row.joinToString(" | ") { it?.toString() ?: "NULL" }
        (holder.itemView as TextView).text = text
        holder.itemView.setOnClickListener { view ->
            showRowDetail(view.context, columns, row)
        }
    }

    private fun showRowDetail(context: android.content.Context, columns: List<String>, row: List<Any?>) {
        val items = columns.mapIndexed { index, col ->
            "$col: ${row.getOrNull(index)?.toString() ?: "NULL"}"
        }.toTypedArray()

        android.app.AlertDialog.Builder(context)
            .setTitle("行详情")
            .setItems(items, null)
            .setNegativeButton("关闭", null)
            .show()
    }
}
