package com.dbftpmanager.ui.connections

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dbftpmanager.R
import com.dbftpmanager.data.model.ConnectionInfo
import com.dbftpmanager.data.model.ConnectionType

class ConnectionAdapter(
    private val onItemClick: (ConnectionInfo) -> Unit,
    private val onMenuClick: (ConnectionInfo, ImageButton) -> Unit
) : RecyclerView.Adapter<ConnectionAdapter.ViewHolder>() {

    private var connections = listOf<ConnectionInfo>()

    fun submitList(list: List<ConnectionInfo>) {
        connections = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_connection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(connections[position])
    }

    override fun getItemCount() = connections.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        private val tvType: TextView = view.findViewById(R.id.tvType)
        private val btnMenu: ImageButton = view.findViewById(R.id.btnMenu)

        fun bind(connection: ConnectionInfo) {
            tvName.text = connection.name
            tvSubtitle.text = connection.displaySubtitle
            tvType.text = connection.type.displayName

            ivIcon.setImageResource(
                when (connection.type) {
                    ConnectionType.MYSQL -> R.drawable.ic_database
                    ConnectionType.POSTGRESQL -> R.drawable.ic_database
                    ConnectionType.SQLSERVER -> R.drawable.ic_database
                    ConnectionType.SQLITE -> R.drawable.ic_database
                    ConnectionType.ORACLE -> R.drawable.ic_database
                    ConnectionType.MARIADB -> R.drawable.ic_database
                    ConnectionType.FTP -> R.drawable.ic_ftp
                    ConnectionType.SFTP -> R.drawable.ic_ftp
                }
            )

            ivIcon.setColorFilter(
                if (connection.isDatabaseType) {
                    ContextCompat.getColor(itemView.context, R.color.db_color)
                } else {
                    ContextCompat.getColor(itemView.context, R.color.ftp_color)
                }
            )

            itemView.setOnClickListener { onItemClick(connection) }
            btnMenu.setOnClickListener { onMenuClick(connection, btnMenu) }
        }
    }
}
