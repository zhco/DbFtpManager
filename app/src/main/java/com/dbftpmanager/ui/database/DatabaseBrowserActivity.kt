package com.dbftpmanager.ui.database

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dbftpmanager.App
import com.dbftpmanager.R
import com.dbftpmanager.data.model.ConnectionInfo
import com.dbftpmanager.data.model.TableInfo
import kotlinx.coroutines.launch

class DatabaseBrowserActivity : AppCompatActivity() {

    private lateinit var viewModel: DatabaseViewModel
    private lateinit var connection: ConnectionInfo
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database_browser)

        viewModel = DatabaseViewModel(application as App)

        connection = intent.getSerializableExtra("connection") as ConnectionInfo

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.title = connection.name
        toolbar.setNavigationOnClickListener {
            viewModel.disconnect()
            finish()
        }

        // Tab buttons
        val btnTabTables = findViewById<android.widget.Button>(R.id.btnTabTables)
        val btnTabSql = findViewById<android.widget.Button>(R.id.btnTabSql)

        // Show tables tab by default
        showTab(0)

        btnTabTables.setOnClickListener {
            showTab(0)
        }

        btnTabSql.setOnClickListener {
            showTab(1)
        }

        // Database Spinner
        val spinnerDb = findViewById<android.widget.Spinner>(R.id.spinnerDatabase)

        lifecycleScope.launch {
            viewModel.databases.collect { databases: List<String> ->
                if (databases.isNotEmpty()) {
                    val adapter = ArrayAdapter(this@DatabaseBrowserActivity,
                        android.R.layout.simple_spinner_item, databases)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerDb.adapter = adapter

                    val listener: android.widget.AdapterView.OnItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                            viewModel.switchDatabase(databases[position])
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    }
                    spinnerDb.onItemSelectedListener = listener
                }
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error: String? ->
                error?.let {
                    Toast.makeText(this@DatabaseBrowserActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Connect
        viewModel.connect(connection)
    }

    private fun showTab(tabIndex: Int) {
        if (currentTab == tabIndex) return
        currentTab = tabIndex

        val btnTabTables = findViewById<android.widget.Button>(R.id.btnTabTables)
        val btnTabSql = findViewById<android.widget.Button>(R.id.btnTabSql)

        btnTabTables.isSelected = (tabIndex == 0)
        btnTabSql.isSelected = (tabIndex == 1)

        val fragment: Fragment = when (tabIndex) {
            0 -> TableListFragment.newInstance(connection)
            1 -> SqlEditorFragment.newInstance(connection)
            else -> TableListFragment.newInstance(connection)
        }

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainer, fragment)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnect()
    }

    companion object {
        fun newIntent(context: Context, connection: ConnectionInfo) =
            Intent(context, DatabaseBrowserActivity::class.java).apply {
                putExtra("connection", connection)
            }
    }
}

class TableListAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<TableListAdapter.ViewHolder>() {

    private var tables = listOf<TableInfo>()

    fun submitList(list: List<TableInfo>) {
        tables = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_table, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tables[position])
    }

    override fun getItemCount() = tables.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTableName: TextView = view.findViewById(R.id.tvTableName)
        private val tvRowCount: TextView = view.findViewById(R.id.tvRowCount)

        fun bind(table: TableInfo) {
            tvTableName.text = table.name
            tvRowCount.text = if (table.rowCount >= 0) "${table.rowCount} 行" else ""
            itemView.setOnClickListener { onItemClick(table.name) }
        }
    }
}
