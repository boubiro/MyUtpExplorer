package info.matpif.myutbexplorer.entities.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import info.matpif.myutbexplorer.entities.UtbAttributes
import info.matpif.myutbexplorer.entities.interfaces.UtbAttributesDao

@Database(entities = [UtbAttributes::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun utbAttributeDao(): UtbAttributesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "MyUtbExplorer.db"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
