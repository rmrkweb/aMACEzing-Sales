package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = SalesTransaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"])
    ]
)
data class PaymentRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: String,
    val amount: Double,
    val paymentDateEpochMillis: Long = System.currentTimeMillis(),
    val paymentMethod: String = "Cash", // Cash, GCash, Bank Transfer, Check, Other
    val referenceNumber: String = "",
    val notes: String = ""
)
