package app.birrbudget.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

internal const val JOINED_SELECT = """
    SELECT t.id AS id, t.amountCents AS amountCents, t.isIncome AS isIncome,
           t.categoryId AS categoryId, t.note AS note, t.epochDay AS epochDay,
           c.name AS categoryName, c.emoji AS categoryEmoji
    FROM transactions t
    JOIN categories c ON c.id = t.categoryId
"""

@Dao
interface BirrDao {
    @Query("SELECT * FROM categories ORDER BY isIncome, name")
    fun categories(): Flow<List<CategoryEntity>>

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    /** REPLACE doubles as "update" when [TransactionEntity.id] is non-zero. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query(
        JOINED_SELECT +
            " WHERE t.epochDay BETWEEN :startDay AND :endDay ORDER BY t.epochDay DESC, t.id DESC",
    )
    fun transactionsBetween(startDay: Long, endDay: Long): Flow<List<TransactionWithCategory>>

    @Query(JOINED_SELECT + " ORDER BY t.epochDay ASC, t.id ASC")
    suspend fun allTransactions(): List<TransactionWithCategory>

    @Query("SELECT * FROM budgets")
    fun budgets(): Flow<List<BudgetEntity>>

    /** Unique index on categoryId means REPLACE overwrites the existing budget for that category. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteBudgetFor(categoryId: Long)
}
