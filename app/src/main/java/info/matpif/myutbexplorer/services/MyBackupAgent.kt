package info.matpif.myutbexplorer.services

import android.app.backup.BackupAgentHelper
import android.app.backup.FileBackupHelper
import android.app.backup.SharedPreferencesBackupHelper


class MyBackupAgent : BackupAgentHelper() {

    private val PREFS = "user_preferences"
    private val PREFS_BACKUP_KEY = "prefs"
    private val DB = "MyUtbExplorer.db"
    private val DB_BACKUP_KEY = "db"

    override fun onCreate() {
        // Allocate a helper and add it to the backup agent
        SharedPreferencesBackupHelper(this, PREFS).also {
            addHelper(PREFS_BACKUP_KEY, it)
        }
        FileBackupHelper(this, DB).also {
            addHelper(DB_BACKUP_KEY, it)
        }
    }
}
