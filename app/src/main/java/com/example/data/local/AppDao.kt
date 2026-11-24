package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
  @Query("SELECT * FROM calls ORDER BY timestamp DESC")
  fun getAllCalls(): Flow<List<CallEntity>>

  @Query("SELECT * FROM calls ORDER BY timestamp DESC LIMIT :limit")
  fun getLatestCalls(limit: Int = 25): Flow<List<CallEntity>>

  @Query("SELECT * FROM calls WHERE id = :id")
  suspend fun getCallById(id: Long): CallEntity?

  @Query("SELECT * FROM calls WHERE status = 'PENDING_REVIEW' ORDER BY escalationDeadline ASC")
  fun getPendingCalls(): Flow<List<CallEntity>>

  @Query("SELECT * FROM calls ORDER BY timestamp DESC")
  suspend fun getAllCallsSnapshot(): List<CallEntity>

  @Query("SELECT * FROM calls WHERE supabaseSynced = 0 ORDER BY timestamp ASC")
  suspend fun getUnsyncedCalls(): List<CallEntity>

  @Query("UPDATE calls SET supabaseSynced = 1 WHERE id = :id")
  suspend fun markSupabaseSynced(id: Long)

  @Query("SELECT * FROM calls WHERE status = 'PENDING_REVIEW' AND escalationDeadline <= :currentTime AND isEscalated = 0")
  suspend fun getCallsNeedingEscalation(currentTime: Long): List<CallEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCall(call: CallEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCalls(calls: List<CallEntity>)

  @Query("SELECT COUNT(*) FROM calls")
  suspend fun getCallCount(): Int

  @Update
  suspend fun updateCall(call: CallEntity)

  @Query("UPDATE calls SET status = 'REVIEWED', isEscalated = 0 WHERE id = :id")
  suspend fun markReviewed(id: Long)

  @Query("UPDATE calls SET status = 'PENDING_REVIEW', isEscalated = 0 WHERE id = :id")
  suspend fun markPending(id: Long)

  @Query("UPDATE calls SET status = 'ESCALATED', isEscalated = 1, escalatedAt = :time, escalationNotes = :notes WHERE id = :id")
  suspend fun markEscalated(id: Long, time: Long, notes: String)

  @Query("DELETE FROM calls WHERE id = :id")
  suspend fun deleteCall(id: Long)

  @Query("DELETE FROM calls")
  suspend fun clearAllCalls()
}

@Dao
interface ProductDao {
  @Query("SELECT * FROM products ORDER BY id ASC")
  fun getAllProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products ORDER BY id ASC")
  suspend fun getAllProductsSnapshot(): List<ProductEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProducts(products: List<ProductEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProduct(product: ProductEntity): Long

  @Update
  suspend fun updateProduct(product: ProductEntity)

  @Query("DELETE FROM products WHERE id = :id")
  suspend fun deleteProduct(id: Long)

  @Query("DELETE FROM products")
  suspend fun deleteAllProducts()

  @Query("SELECT COUNT(*) FROM products")
  suspend fun getCount(): Int

  @Query("SELECT * FROM products WHERE name LIKE '%Pipeline%' LIMIT 1")
  suspend fun findOldSurfboardSample(): ProductEntity?
}

@Dao
interface FaqDao {
  @Query("SELECT * FROM business_faq ORDER BY id ASC")
  fun getAllFaqs(): Flow<List<FaqEntity>>

  @Query("SELECT * FROM business_faq ORDER BY id ASC")
  suspend fun getAllFaqsSnapshot(): List<FaqEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFaqs(faqs: List<FaqEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFaq(faq: FaqEntity): Long

  @Query("DELETE FROM business_faq")
  suspend fun deleteAllFaqs()

  @Query("SELECT COUNT(*) FROM business_faq")
  suspend fun getCount(): Int
}

@Dao
interface WaveAuditLeadDao {
  @Query("SELECT * FROM wave_audit_leads ORDER BY createdAt DESC")
  fun getAllLeads(): Flow<List<WaveAuditLeadEntity>>

  @Query("SELECT * FROM wave_audit_leads ORDER BY createdAt DESC")
  suspend fun getAllLeadsSnapshot(): List<WaveAuditLeadEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLead(lead: WaveAuditLeadEntity)
}
