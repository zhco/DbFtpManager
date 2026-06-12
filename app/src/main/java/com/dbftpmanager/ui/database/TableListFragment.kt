package com.dbftpmanager.ui.database

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dbftpmanager.R
import com.dbftpmanager.data.model.ConnectionInfo
import kotlinx.coroutines.launch

class TableListFragment : Fragment() {

    private val viewModel: DatabaseViewModel by viewModels({ requireActivity() })
    private lateinit var tableAdapter: TableListAdapter
    private lateinit var connection: ConnectionInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connection = arguments?.getSerializable("connection") as ConnectionInfo
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_connection_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyState = view.findViewById<View>(R.id.emptyState)

        tableAdapter = TableListAdapter { tableName ->
            val intent = Intent(requireContext(), TableDataActivity::class.java).apply {
                putExtra("connection", connection)
                putExtra("table_name", tableName)
            }
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = tableAdapter

        lifecycleScope.launch {
            viewModel.tables.collect { tables ->
                tableAdapter.submitList(tables)
                emptyState.visibility = if (tables.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    companion object {
        fun newInstance(connection: ConnectionInfo) = TableListFragment().apply {
            arguments = Bundle().apply {
                putSerializable("connection", connection)
            }
        }
    }
}
