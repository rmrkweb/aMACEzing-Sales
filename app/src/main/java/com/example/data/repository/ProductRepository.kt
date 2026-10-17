package com.example.data.repository

import com.example.data.firebase.FirestoreProduct
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing Product catalog records in Firestore.
 * Preserves historical unit costs and selling prices.
 */
interface ProductRepository {
    fun observeProducts(): Flow<List<FirestoreProduct>>
    suspend fun getProducts(): List<FirestoreProduct>
    suspend fun getProductById(productId: String): FirestoreProduct?
    suspend fun saveProduct(product: FirestoreProduct)
    suspend fun deleteProduct(productId: String)
}
