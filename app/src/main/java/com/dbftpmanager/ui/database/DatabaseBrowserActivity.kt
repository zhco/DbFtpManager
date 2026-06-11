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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.FragmentStateAdapter
import com.dbftpmanager.R
import com.dbftpmanager.data.model.ConnectionInfo
import com.dbftpmanager.data.model.TableInfo
import kotlinx.coroutines.launch

class DatabaseBrowserActivity : AppCompatActivity() {

    private lateinit var viewModel: DatabaseViewModel
    private lateinit var connection: ConnectionInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database_browser)

        connection = intent.getSerializableExtra("connection") as ConnectionInfo

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.title = connection.name
        toolbar.setNavigationOnClickListener {
            viewModel.disconnect()
            finish()
        }

        viewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]

        // Tab + ViewPager
        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)

        viewPager.adapter = DatabasePagerAdapter(this, connection)
        com.google.android.material.tabs.TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tables)
                1 -> getString(R.string.sql_editor)
                else -> ""
            }
        }.attach()

        // 数据库选择 Spinner
        val spinnerDb = findViewById<android.widget.Spinner>(R.id.spinnerDatabase)

        lifecycleScope.launch {
            viewModel.databases.collect { databases ->
                if (databases.isNotEmpty()) {
                    val adapter = ArrayAdapter(this@DatabaseBrowserActivity,
                        android.R.layout.simple_spinner_item, databases)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerDb.adapter = adapter

                    spinnerDb.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                            viewModel.switchDatabase(databases[position])
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(this@DatabaseBrowserActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 连接
        viewModel.connect(connection)
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

class DatabasePagerAdapter(
    fragmentActivity: FragmentActivity,
    private val connection: ConnectionInfo
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TableListFragment.newInstance(connection)
            1 -> SqlEditorFragment.newInstance(connection)
            else -> TableListFragment.newInstance(connection)
        }
    }
}
