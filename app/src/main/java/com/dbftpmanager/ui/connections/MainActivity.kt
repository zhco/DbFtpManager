package com.dbftpmanager.ui.connections

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.dbftpmanager.R
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.title_connections)

        val btnTabDatabase = findViewById<android.widget.Button>(R.id.btnTabDatabase)
        val btnTabFtp = findViewById<android.widget.Button>(R.id.btnTabFtp)

        // Show database tab by default
        showTab(0)

        btnTabDatabase.setOnClickListener {
            showTab(0)
        }

        btnTabFtp.setOnClickListener {
            showTab(1)
        }
    }

    private fun showTab(tabIndex: Int) {
        if (currentTab == tabIndex) return
        currentTab = tabIndex

        val btnTabDatabase = findViewById<android.widget.Button>(R.id.btnTabDatabase)
        val btnTabFtp = findViewById<android.widget.Button>(R.id.btnTabFtp)

        // Update button states
        btnTabDatabase.isSelected = (tabIndex == 0)
        btnTabFtp.isSelected = (tabIndex == 1)

        val fragment: Fragment = when (tabIndex) {
            0 -> ConnectionListFragment.newInstance(ConnectionListFragment.FilterType.DATABASE)
            1 -> ConnectionListFragment.newInstance(ConnectionListFragment.FilterType.FTP)
            else -> ConnectionListFragment.newInstance(ConnectionListFragment.FilterType.ALL)
        }

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainer, fragment)
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
