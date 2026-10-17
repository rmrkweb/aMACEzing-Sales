package com.example.data.repository

import com.example.data.firebase.FirestoreCustomer
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing Customer records in Firestore.
 */
interface CustomerRepository {
    fun observeCustomers(): Flow<List<FirestoreCustomer>>
    suspend fun getCustomers(): List<FirestoreCustomer>
    suspend fun getCustomerById(customerId: String): FirestoreCustomer?
    suspend fun saveCustomer(customer: FirestoreCustomer)
    suspend fun deleteCustomer(customerId: String)
}
