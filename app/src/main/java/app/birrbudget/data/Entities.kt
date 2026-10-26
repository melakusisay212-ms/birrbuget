package app.birrbudget.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val isIncome: Boolean,
)

/**
 * Dates are stored as Gregorian epoch days (days since 1970-01-01), independent of which
 * calendar the user views. Amounts are positive integer cents (santim); [isIncome] gives the sign.
 */
@Entity(
    tableName = "transactions",
    indices = [Index("epochDay"), Index("categoryId")],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val isIncome: Boolean,
    val categoryId: Long,
    val note: String,
    val epochDay: Long,
)

/** One monthly spending limit per expense category. */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val limitCents: Long,
)

/** Query result: a transaction joined with its category. Property names match the SQL aliases. */
data class TransactionWithCategory(
    val id: Long,
    val amountCents: Long,
    val isIncome: Boolean,
    val categoryId: Long,
    val note: String,
    val epochDay: Long,
    val categoryName: String,
    val categoryEmoji: String,
)
