package com.example.model

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionWithDetails(
    @Embedded
    val transaction: SalesTransaction,

    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val lineItems: List<TransactionLineItem> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val payments: List<PaymentRecord> = emptyList()
) {
    val totalItemsCount: Int
        get() = lineItems.sumOf { it.quantity }
}
