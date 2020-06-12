package info.matpif.myutbexplorer.entities.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import info.matpif.myutbexplorer.entities.DownloadUploadManager
import info.matpif.myutbexplorer.entities.UtbAttributes
import info.matpif.myutbexplorer.entities.interfaces.DownloadUploadManagerDao
import info.matpif.myutbexplorer.entities.interfaces.UtbAttributesDao

@Database(version = 5, entities = [UtbAttributes::class, DownloadUploadManager::class])
abstract class AppDatabase : RoomDatabase() {
    abstract fun utbAttributeDao(): UtbAttributesDao
    abstract fun downloadUploadManagerDao(): DownloadUploadManagerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `download_upload_manager` (`uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id_request` INTEGER NOT NULL, `file_name` TEXT, `progress` INTEGER)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `download_upload_manager` ADD COLUMN `download` INTEGER")
                database.execSQL("ALTER TABLE `download_upload_manager` ADD COLUMN `create_at` TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `download_upload_manager` ADD COLUMN `file_code` TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `download_upload_manager` ADD COLUMN `tag_upload` TEXT")
            }
        }

        fun getDatabase(
            context: Context
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "MyUtbExplorer.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
