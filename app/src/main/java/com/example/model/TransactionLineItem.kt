package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "line_items",
    foreignKeys = [
        ForeignKey(
            entity = SalesTransaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["productId"])
    ]
)
data class TransactionLineItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: String,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitSellingPrice: Double, // Captured snapshot at time of sale
    val unitCost: Double,         // Captured snapshot at time of sale
    val lineSubtotal: Double      // quantity * unitSellingPrice
)
