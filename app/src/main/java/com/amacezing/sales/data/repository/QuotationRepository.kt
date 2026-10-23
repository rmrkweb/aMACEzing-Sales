package com.amacezing.sales.data.repository

import com.amacezing.sales.data.firebase.FirestoreQuotation

/** Cloud storage for quotations (users/{uid}/quotations, one document per quotation number). */
interface QuotationRepository {
    suspend fun getQuotations(): List<FirestoreQuotation>
    suspend fun saveQuotation(quotation: FirestoreQuotation)
    suspend fun deleteQuotation(quotationId: String)
}
