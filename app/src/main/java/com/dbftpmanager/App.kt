package com.dbftpmanager

import android.app.Application
import com.dbftpmanager.data.local.AppDatabase
import com.dbftpmanager.data.repository.ConnectionRepository
import com.dbftpmanager.data.repository.QueryHistoryRepository

class App : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val connectionRepository: ConnectionRepository by lazy { ConnectionRepository(database.connectionDao()) }
    val queryHistoryRepository: QueryHistoryRepository by lazy { QueryHistoryRepository(database.queryHistoryDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
