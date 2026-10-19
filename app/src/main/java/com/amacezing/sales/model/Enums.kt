package com.amacezing.sales.model

enum class BusinessType(val displayName: String, val prefix: String) {
    ENGRAVING("Engraving", "ENG"),
    SINTRA_BOARD("Sintra Board", "SNT")
}

enum class PaymentStatus(val displayName: String) {
    PENDING("Pending"),
    PARTIALLY_PAID("Partially Paid"),
    PAID("Paid"),
    CANCELLED("Cancelled")
}

enum class DiscountType(val displayName: String) {
    PERCENTAGE("Percentage (%)"),
    FIXED("Fixed Amount (₱)")
}

enum class QuotationStatus(val displayName: String) {
    DRAFT("Draft"),
    SENT("Sent to Client"),
    ACCEPTED("Accepted"),
    DECLINED("Declined"),
    CONVERTED("Converted to Order")
}

