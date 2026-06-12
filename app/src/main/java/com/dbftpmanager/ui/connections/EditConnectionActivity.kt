package com.dbftpmanager.ui.connections

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dbftpmanager.App
import com.dbftpmanager.R
import com.dbftpmanager.data.model.ConnectionInfo
import com.dbftpmanager.data.model.ConnectionType
import com.dbftpmanager.util.DatabaseManager
import com.dbftpmanager.util.FtpClientManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditConnectionActivity : AppCompatActivity() {

    private var connectionId: Long = 0
    private var isEditing = false

    private lateinit var etName: AutoCompleteTextView
    private lateinit var actvType: AutoCompleteTextView
    private lateinit var etHost: AutoCompleteTextView
    private lateinit var etPort: AutoCompleteTextView
    private lateinit var etUsername: AutoCompleteTextView
    private lateinit var etPassword: AutoCompleteTextView
    private lateinit var etDatabase: AutoCompleteTextView
    private lateinit var tilDatabase: com.google.android.material.textfield.TextInputLayout

    private val typeList = ConnectionType.values().map { it.displayName }
    private var selectedType: ConnectionType = ConnectionType.MYSQL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_connection)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.title = if (isEditing) getString(R.string.edit_connection) else getString(R.string.add_connection)
        toolbar.setNavigationOnClickListener { finish() }

        initViews()
        setupTypeDropdown()

        connectionId = intent.getLongExtra("connection_id", 0)
        if (connectionId > 0) {
            isEditing = true
            toolbar.title = getString(R.string.edit_connection)
            loadConnection()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave).setOnClickListener {
            saveConnection()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTest).setOnClickListener {
            testConnection()
        }
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        actvType = findViewById(R.id.actvType)
        etHost = findViewById(R.id.etHost)
        etPort = findViewById(R.id.etPort)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etDatabase = findViewById(R.id.etDatabase)
        tilDatabase = findViewById(R.id.tilDatabase)
    }

    private fun setupTypeDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, typeList)
        actvType.setAdapter(adapter)
        actvType.setText(typeList.first(), false)
        etPort.setText(ConnectionType.MYSQL.defaultPort.toString())

        actvType.setOnItemClickListener { _, _, position, _ ->
            val typeName = typeList[position]
            selectedType = ConnectionType.values().first { it.displayName == typeName }
            etPort.setText(selectedType.defaultPort.toString())
            updateVisibility()
        }
    }

    private fun updateVisibility() {
        val isDb = selectedType in listOf(
            ConnectionType.MYSQL, ConnectionType.POSTGRESQL,
            ConnectionType.SQLSERVER, ConnectionType.ORACLE, ConnectionType.MARIADB
        )
        tilDatabase.visibility = if (isDb || selectedType == ConnectionType.SQLITE) View.VISIBLE else View.GONE
        if (selectedType == ConnectionType.SQLITE) {
            etHost.hint = "本地文件路径"
            etPort.setText("0")
        } else {
            etHost.hint = getString(R.string.host)
        }
    }

    private fun loadConnection() {
        lifecycleScope.launch {
            val connection = withContext(Dispatchers.IO) {
                (application as App).connectionRepository.getConnectionById(connectionId)
            }
            connection?.let {
                etName.setText(it.name)
                actvType.setText(it.type.displayName, false)
                selectedType = it.type
                etHost.setText(it.host)
                etPort.setText(it.port.toString())
                etUsername.setText(it.username)
                etPassword.setText(it.password)
                etDatabase.setText(it.databaseName)
                updateVisibility()
            }
        }
    }

    private fun saveConnection() {
        val name = etName.text.toString().trim()
        val host = etHost.text.toString().trim()
        val port = etPort.text.toString().trim().toIntOrNull() ?: selectedType.defaultPort
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val database = etDatabase.text.toString().trim()

        if (name.isEmpty() || (selectedType != ConnectionType.SQLITE && host.isEmpty())) {
            Toast.makeText(this, R.string.please_fill_required, Toast.LENGTH_SHORT).show()
            return
        }

        val connection = ConnectionInfo(
            id = connectionId,
            name = name,
            type = selectedType,
            host = host,
            port = port,
            username = username,
            password = password,
            databaseName = database,
            updatedAt = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (isEditing) {
                        (application as App).connectionRepository.updateConnection(connection)
                    } else {
                        (application as App).connectionRepository.saveConnection(connection)
                    }
                }
                Toast.makeText(this@EditConnectionActivity, R.string.connection_saved, Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@EditConnectionActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testConnection() {
        val host = etHost.text.toString().trim()
        val port = etPort.text.toString().trim().toIntOrNull() ?: selectedType.defaultPort
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val database = etDatabase.text.toString().trim()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                if (ConnectionInfo(type = selectedType).isDatabaseType) {
                    val dbManager = DatabaseManager()
                    dbManager.testConnection(selectedType, host, port, username, password, database)
                } else {
                    val ftpManager = FtpClientManager()
                    val conn = ConnectionInfo(
                        name = "", type = selectedType, host = host, port = port,
                        username = username, password = password
                    )
                    ftpManager.testConnection(conn)
                }
            }
            Toast.makeText(
                this@EditConnectionActivity,
                if (result.first) result.second else "失败: ${result.second}",
                if (result.first) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        fun newIntent(context: Context, connectionId: Long = 0) =
            Intent(context, EditConnectionActivity::class.java).apply {
                putExtra("connection_id", connectionId)
            }
    }
}
