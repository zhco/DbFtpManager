package com.dbftpmanager.ui.ftp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dbftpmanager.App
import com.dbftpmanager.R
import com.dbftpmanager.data.model.ConnectionInfo
import com.dbftpmanager.data.model.FtpFileEntry
import com.dbftpmanager.ui.viewmodel.FtpViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FtpBrowserActivity : AppCompatActivity() {

    private lateinit var viewModel: FtpViewModel
    private lateinit var adapter: FileListAdapter
    private lateinit var connection: ConnectionInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ftp_browser)

        viewModel = FtpViewModel(application as App)

        connection = intent.getSerializableExtra("connection") as ConnectionInfo

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.title = connection.name
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val swipeRefresh = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        val tvPath = findViewById<TextView>(R.id.tvCurrentPath)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)

        adapter = FileListAdapter(
            onItemClick = { file: FtpFileEntry ->
                if (file.isDirectory) {
                    viewModel.navigateTo(file.fullPath)
                }
            },
            onItemMenuClick = { file: FtpFileEntry, view: View ->
                showFileMenu(file, view)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabUpload)
            .setOnClickListener { pickFile() }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabCreateFolder)
            .setOnClickListener { showCreateFolderDialog() }

        // Observe data
        lifecycleScope.launch {
            viewModel.files.collect { files: List<FtpFileEntry> ->
                adapter.submitList(files)
                swipeRefresh.isRefreshing = false
            }
        }

        lifecycleScope.launch {
            viewModel.currentPath.collect { path: String ->
                tvPath.text = path
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { loading: Boolean ->
                progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error: String? ->
                error?.let {
                    Toast.makeText(this@FtpBrowserActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.message.collect { msg: String? ->
                msg?.let {
                    Toast.makeText(this@FtpBrowserActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
        }

        // Connect
        viewModel.connect(connection)
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_PICK_FILE)
    }

    @Deprecated("Use Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri: Uri ->
                uploadFile(uri)
            }
        }
    }

    private fun uploadFile(uri: Uri) {
        val fileName = getFileNameFromUri(uri) ?: "unknown_file"
        lifecycleScope.launch {
            try {
                val tempFile = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val file = File(cacheDir, fileName)
                        file.outputStream().use { output ->
                            inputStream.copyTo(output)
                        }
                        file
                    }
                }
                if (tempFile != null && tempFile.exists()) {
                    viewModel.uploadFile(tempFile.absolutePath, fileName) {
                        tempFile.delete()
                    }
                } else {
                    Toast.makeText(this@FtpBrowserActivity, "文件读取失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FtpBrowserActivity, "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.lastPathSegment?.substringAfterLast("/")
        }
        return result
    }

    private fun showCreateFolderDialog() {
        val input = android.widget.EditText(this)
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.create_folder)
            .setView(input)
            .setPositiveButton(R.string.ok) { _: android.content.DialogInterface, _: Int ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createFolder(name)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showFileMenu(file: FtpFileEntry, anchor: View) {
        val popup = PopupMenu(this, anchor)
        if (file.isDirectory) {
            popup.menu.add(0, MENU_DELETE, 0, R.string.delete_file)
        } else {
            popup.menu.add(0, MENU_DOWNLOAD, 0, R.string.download_file)
            popup.menu.add(0, MENU_DELETE, 0, R.string.delete_file)
            popup.menu.add(0, MENU_RENAME, 0, R.string.rename)
        }
        popup.setOnMenuItemClickListener { item: android.view.MenuItem ->
            when (item.itemId) {
                MENU_DOWNLOAD -> downloadFile(file)
                MENU_DELETE -> confirmDelete(file)
                MENU_RENAME -> showRenameDialog(file)
            }
            true
        }
        popup.show()
    }

    private fun downloadFile(file: FtpFileEntry) {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val localPath = File(downloadsDir, file.name).absolutePath
        viewModel.downloadFile(file.fullPath, localPath)
    }

    private fun confirmDelete(file: FtpFileEntry) {
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_file)
            .setMessage(file.name)
            .setPositiveButton(R.string.ok) { _: android.content.DialogInterface, _: Int ->
                viewModel.deleteFile(file.fullPath, file.isDirectory)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showRenameDialog(file: FtpFileEntry) {
        val input = android.widget.EditText(this)
        input.setText(file.name)
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.rename)
            .setView(input)
            .setPositiveButton(R.string.ok) { _: android.content.DialogInterface, _: Int ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val parentPath = file.fullPath.substringBeforeLast("/")
                    viewModel.renameFile(file.fullPath, "$parentPath/$newName")
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onBackPressed() {
        if (viewModel.currentPath.value != "/") {
            viewModel.navigateUp()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val REQUEST_PICK_FILE = 1001
        private const val MENU_DOWNLOAD = 1
        private const val MENU_DELETE = 2
        private const val MENU_RENAME = 3

        fun newIntent(context: Context, connection: ConnectionInfo) =
            Intent(context, FtpBrowserActivity::class.java).apply {
                putExtra("connection", connection)
            }
    }
}

class FileListAdapter(
    private val onItemClick: (FtpFileEntry) -> Unit,
    private val onItemMenuClick: (FtpFileEntry, View) -> Unit
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    private var files = listOf<FtpFileEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun submitList(list: List<FtpFileEntry>) {
        files = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount() = files.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        private val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        private val tvFileDetails: TextView = view.findViewById(R.id.tvFileDetails)
        private val btnMore: ImageView = view.findViewById(R.id.btnMore)

        fun bind(file: FtpFileEntry) {
            tvFileName.text = file.name

            if (file.isDirectory) {
                ivIcon.setImageResource(android.R.drawable.ic_menu_view)
                ivIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.primary))
                tvFileDetails.text = file.permissions
            } else {
                ivIcon.setImageResource(android.R.drawable.ic_menu_save)
                ivIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                tvFileDetails.text = "${file.formattedSize}  |  ${if (file.lastModified > 0) dateFormat.format(Date(file.lastModified)) else ""}"
            }

            itemView.setOnClickListener { onItemClick(file) }
            btnMore.setOnClickListener { onItemMenuClick(file, btnMore) }
        }
    }
}
