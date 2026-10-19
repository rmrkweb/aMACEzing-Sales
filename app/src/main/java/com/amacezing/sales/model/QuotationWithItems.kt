package com.amacezing.sales.model

import androidx.room.Embedded
import androidx.room.Relation

data class QuotationWithItems(
    @Embedded
    val quotation: Quotation,

    @Relation(
        parentColumn = "id",
        entityColumn = "quotationId"
    )
    val lineItems: List<QuotationLineItem> = emptyList()
) {
    val totalItemsCount: Int
        get() = lineItems.sumOf { it.quantity }
}
