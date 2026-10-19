package com.amacezing.sales.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quotations",
    indices = [
        Index(value = ["quotationNumber"], unique = true),
        Index(value = ["businessType"]),
        Index(value = ["quotationDateEpochMillis"])
    ]
)
data class Quotation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quotationNumber: String, // e.g. QT-2026-0001
    val businessType: BusinessType,
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val quotationDateEpochMillis: Long = System.currentTimeMillis(),
    val validityDays: Int = 7,
    val validUntilEpochMillis: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discountValue: Double = 0.0,
    val grossTotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val netTotal: Double = 0.0,
    val status: QuotationStatus = QuotationStatus.DRAFT,
    val remarks: String = "",
    val convertedTransactionId: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)
