package com.dbftpmanager.ui.connections

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dbftpmanager.App
import com.dbftpmanager.R
import com.dbftpmanager.data.model.ConnectionInfo
import kotlinx.coroutines.launch

class ConnectionListFragment : Fragment() {

    private var filterType: FilterType = FilterType.ALL
    private lateinit var adapter: ConnectionAdapter

    enum class FilterType { ALL, DATABASE, FTP }

    companion object {
        fun newInstance(filterType: FilterType) = ConnectionListFragment().apply {
            this.filterType = filterType
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_connection_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        val fabAdd = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd)
        val emptyState = view.findViewById<LinearLayout>(R.id.emptyState)
        val btnAddEmpty = emptyState.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAdd)

        adapter = ConnectionAdapter(
            onItemClick = { connection: ConnectionInfo -> openConnection(connection) },
            onMenuClick = { connection: ConnectionInfo, btn: View -> showPopupMenu(connection, btn) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val repository = (requireActivity().application as App).connectionRepository

        lifecycleScope.launch {
            repository.allConnections.collect { connections: List<ConnectionInfo> ->
                val filtered: List<ConnectionInfo> = when (filterType) {
                    FilterType.ALL -> connections
                    FilterType.DATABASE -> connections.filter { it.isDatabaseType }
                    FilterType.FTP -> connections.filter { it.isFtpType }
                }
                adapter.submitList(filtered)
                emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        val addClick: (View) -> Unit = {
            startActivity(Intent(requireContext(), EditConnectionActivity::class.java))
        }
        fabAdd.setOnClickListener(addClick)
        btnAddEmpty.setOnClickListener(addClick)
    }

    private fun openConnection(connection: ConnectionInfo) {
        val intent = if (connection.isDatabaseType) {
            com.dbftpmanager.ui.database.DatabaseBrowserActivity.newIntent(requireContext(), connection)
        } else {
            com.dbftpmanager.ui.ftp.FtpBrowserActivity.newIntent(requireContext(), connection)
        }
        startActivity(intent)
    }

    private fun showPopupMenu(connection: ConnectionInfo, anchor: View) {
        val popup = android.widget.PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_connection_item, popup.menu)
        popup.setOnMenuItemClickListener { item: android.view.MenuItem ->
            when (item.itemId) {
                R.id.action_edit -> {
                    startActivity(
                        Intent(requireContext(), EditConnectionActivity::class.java).apply {
                            putExtra("connection_id", connection.id)
                        }
                    )
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirm(connection)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirm(connection: ConnectionInfo) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete)
            .setPositiveButton(R.string.ok) { _: android.content.DialogInterface, _: Int ->
                lifecycleScope.launch {
                    (requireActivity().application as App).connectionRepository.deleteConnection(connection)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
