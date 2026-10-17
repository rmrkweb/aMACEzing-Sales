package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["orderNumber"], unique = false),
        Index(value = ["businessType"]),
        Index(value = ["orderDateEpochMillis"])
    ]
)
data class SalesTransaction(
    @PrimaryKey
    val id: String, // UUID
    val orderNumber: String, // e.g. ENG-2026-0001
    val businessType: BusinessType,
    val customerId: Long? = null,
    val customerName: String,
    val orderDateEpochMillis: Long,
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discountPercentage: Double = 0.0,
    val discountAmount: Double = 0.0,
    val grossSales: Double = 0.0,
    val netSales: Double = 0.0,
    val estimatedProfit: Double = 0.0,
    val totalProductCost: Double = 0.0,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val amountPaid: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val paymentNotes: String = "",
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
