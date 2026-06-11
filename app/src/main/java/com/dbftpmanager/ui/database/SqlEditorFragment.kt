package com.dbftpmanager.ui.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dbftpmanager.R
import com.dbftpmanager.data.model.ConnectionInfo
import kotlinx.coroutines.launch

class SqlEditorFragment : Fragment() {

    private lateinit var viewModel: DatabaseViewModel
    private lateinit var connection: ConnectionInfo
    private lateinit var resultAdapter: QueryResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connection = arguments?.getSerializable("connection") as ConnectionInfo
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_sql_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[DatabaseViewModel::class.java]

        val etSql = view.findViewById<EditText>(R.id.etSqlInput)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val tvResultInfo = view.findViewById<TextView>(R.id.tvResultInfo)
        val resultInfo = view.findViewById<View>(R.id.resultInfo)
        val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.progressBar)

        resultAdapter = QueryResultAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = resultAdapter

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExecute).setOnClickListener {
            val sql = etSql.text.toString().trim()
            if (sql.isNotEmpty()) {
                viewModel.executeQuery(sql)
            }
        }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClear).setOnClickListener {
            etSql.text.clear()
            resultAdapter.submitData(emptyList(), emptyList())
            tvError.visibility = View.GONE
            tvEmpty.visibility = View.GONE
            resultInfo.visibility = View.GONE
        }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHistory).setOnClickListener {
            showHistoryDialog(etSql)
        }

        lifecycleScope.launch {
            viewModel.queryResult.collect { result ->
                progressBar.visibility = View.GONE
                if (result == null) return@collect

                if (result.isSuccess) {
                    tvError.visibility = View.GONE
                    if (result.isSelect) {
                        if (result.rows.isNotEmpty()) {
                            resultAdapter.submitData(result.columns, result.rows)
                            recyclerView.visibility = View.VISIBLE
                            tvEmpty.visibility = View.GONE
                            resultInfo.visibility = View.VISIBLE
                            tvResultInfo.text = "${result.rowCount} 行  |  耗时: ${result.executionTimeMs} ms"
                        } else {
                            recyclerView.visibility = View.GONE
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "查询结果为空"
                            resultInfo.visibility = View.VISIBLE
                            tvResultInfo.text = "0 行  |  耗时: ${result.executionTimeMs} ms"
                        }
                    } else {
                        recyclerView.visibility = View.GONE
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "执行成功，影响 ${result.affectedRows} 行"
                        resultInfo.visibility = View.VISIBLE
                        tvResultInfo.text = "影响行数: ${result.affectedRows}  |  耗时: ${result.executionTimeMs} ms"
                    }
                } else {
                    tvError.visibility = View.VISIBLE
                    tvError.text = result.error
                    recyclerView.visibility = View.GONE
                    tvEmpty.visibility = View.GONE
                    resultInfo.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                if (loading) progressBar.visibility = View.VISIBLE
            }
        }
    }

    private fun showHistoryDialog(etSql: EditText) {
        val history = viewModel.sqlHistory.value
        if (history.isEmpty()) {
            Toast.makeText(requireContext(), "暂无历史记录", Toast.LENGTH_SHORT).show()
            return
        }

        val items = history.map { it.sql }.toTypedArray()
        val connectionId = history.first().connectionId
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("SQL 历史记录")
            .setItems(items) { _, which ->
                etSql.setText(items[which])
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    companion object {
        fun newInstance(connection: ConnectionInfo) = SqlEditorFragment().apply {
            arguments = Bundle().apply {
                putSerializable("connection", connection)
            }
        }
    }
}

class QueryResultAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var columns = listOf<String>()
    private var rows = listOf<List<Any?>>()

    fun submitData(cols: List<String>, data: List<List<Any?>>) {
        columns = cols
        rows = data
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (rows.isEmpty()) 0 else rows.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                RowViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val tv = holder.itemView as TextView
        when (holder) {
            is HeaderViewHolder -> {
                tv.text = columns.joinToString(" | ")
                tv.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.primary_light))
                tv.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            }
            is RowViewHolder -> {
                val rowIndex = position - 1
                if (rowIndex in rows.indices) {
                    tv.text = rows[rowIndex].joinToString(" | ") { it?.toString() ?: "NULL" }
                }
            }
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)
    class RowViewHolder(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
    }
}
