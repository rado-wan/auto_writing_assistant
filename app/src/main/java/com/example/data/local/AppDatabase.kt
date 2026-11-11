package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.SavedText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [SavedText::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedTextDao(): SavedTextDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "writing_assistant.db"
                ).fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.savedTextDao()?.insertAll(getDefaultTemplates())
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        fun getDefaultTemplates(): List<SavedText> {
            return listOf(
                SavedText(
                    id = 1,
                    title = "Email Sign-off",
                    body = "Best regards,\nWriting Assistant",
                    isFormattingEnabled = false,
                    orderIndex = 0
                ),
                SavedText(
                    id = 2,
                    title = "Meeting Notes Header",
                    body = "Meeting Notes - {date} ({day})\nAttendees:\n- \nAgenda:\n",
                    isFormattingEnabled = false,
                    orderIndex = 1
                ),
                SavedText(
                    id = 3,
                    title = "Timestamp Stamp",
                    body = "[{date:dd-MM-yyyy} {time:HH:mm}] ",
                    isFormattingEnabled = false,
                    orderIndex = 2
                ),
                SavedText(
                    id = 4,
                    title = "Daily Journal Log",
                    body = "Entry on {date:dd MMMM yyyy} ({day:EEEE}):\n",
                    isFormattingEnabled = false,
                    orderIndex = 3
                )
            )
        }
    }
}
