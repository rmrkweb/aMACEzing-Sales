package com.amacezing.sales.data.repository

import com.amacezing.sales.data.firebase.FirestoreCustomer
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
