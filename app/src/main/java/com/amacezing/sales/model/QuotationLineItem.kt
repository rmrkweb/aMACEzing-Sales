package com.amacezing.sales.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quotation_line_items",
    foreignKeys = [
        ForeignKey(
            entity = Quotation::class,
            parentColumns = ["id"],
            childColumns = ["quotationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["quotationId"]),
        Index(value = ["productId"])
    ]
)
data class QuotationLineItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quotationId: Long,
    val productId: Long? = null,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineSubtotal: Double = quantity * unitPrice,
    val notes: String = ""
)
