package app.birrbudget.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CategoryEntity::class, TransactionEntity::class, BudgetEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class BirrDatabase : RoomDatabase() {
    abstract fun dao(): BirrDao

    companion object {
        @Volatile
        private var instance: BirrDatabase? = null

        fun get(context: Context): BirrDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): BirrDatabase =
            Room.databaseBuilder(context, BirrDatabase::class.java, "birr-budget.db")
                .addCallback(SeedCallback)
                .build()
    }

    private object SeedCallback : RoomDatabase.Callback() {
        // name, emoji, isIncome
        private val defaults = listOf(
            Triple("Food", "🍽️", false),
            Triple("Transport", "🚌", false),
            Triple("Rent", "🏠", false),
            Triple("Bills & Telecom", "📱", false),
            Triple("Health", "💊", false),
            Triple("Education", "📚", false),
            Triple("Shopping", "🛍️", false),
            Triple("Equb / Iddir", "🤝", false),
            Triple("Family", "👨‍👩‍👧", false),
            Triple("Other", "📦", false),
            Triple("Salary", "💼", true),
            Triple("Business", "🏪", true),
            Triple("Gift", "🎁", true),
            Triple("Other income", "💰", true),
        )

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            for ((name, emoji, isIncome) in defaults) {
                db.execSQL(
                    "INSERT INTO categories (name, emoji, isIncome) VALUES (?, ?, ?)",
                    arrayOf<Any>(name, emoji, if (isIncome) 1 else 0),
                )
            }
        }
    }
}
