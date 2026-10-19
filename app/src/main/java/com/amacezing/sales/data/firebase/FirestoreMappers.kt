package com.amacezing.sales.data.firebase

import com.amacezing.sales.model.AppSettings
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.Customer
import com.amacezing.sales.model.DiscountType
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.model.Product
import com.amacezing.sales.model.SalesTransaction
import java.util.UUID

/**
 * Data mapping and robust edge-case parsing between Room entities and Cloud Firestore models.
 */
object FirestoreMappers {

    // --- Product Mapping & Parsing ---

    fun Product.toFirestore(userId: String): FirestoreProduct {
        return FirestoreProduct(
            id = id.toString(),
            userId = userId,
            name = name.trim(),
            businessType = businessType.name,
            sellingPrice = sellingPrice.coerceAtLeast(0.0),
            unitCost = unitCost.coerceAtLeast(0.0),
            isActive = isActive
        )
    }

    fun FirestoreProduct.toRoomProduct(): Product {
        val safeBusinessType = parseBusinessType(businessType)
        val safeSellingPrice = if (sellingPrice.isNaN() || sellingPrice < 0.0) 0.0 else sellingPrice
        val safeUnitCost = if (unitCost.isNaN() || unitCost < 0.0) 0.0 else unitCost
        val safeId = id.toLongOrNull() ?: 0L

        return Product(
            id = safeId,
            name = name.trim().ifEmpty { "Unnamed Product" },
            businessType = safeBusinessType,
            sellingPrice = safeSellingPrice,
            unitCost = safeUnitCost,
            isActive = isActive
        )
    }

    // --- Customer Mapping & Parsing ---

    fun Customer.toFirestore(userId: String): FirestoreCustomer {
        return FirestoreCustomer(
            id = id.toString(),
            userId = userId,
            name = name.trim(),
            phone = phone.trim(),
            email = email.trim(),
            notes = notes.trim()
        )
    }

    fun FirestoreCustomer.toRoomCustomer(): Customer {
        val safeId = id.toLongOrNull() ?: 0L
        return Customer(
            id = safeId,
            name = name.trim().ifEmpty { "Walk-in Customer" },
            phone = phone.trim(),
            email = email.trim(),
            notes = notes.trim()
        )
    }

    // --- Transaction Mapping & Parsing ---

    fun SalesTransaction.toFirestore(userId: String, summary: String = ""): FirestoreTransaction {
        return FirestoreTransaction(
            id = id.ifBlank { UUID.randomUUID().toString() },
            userId = userId,
            orderNumber = orderNumber.trim(),
            businessType = businessType.name,
            customerName = customerName.trim(),
            orderDateEpochMillis = if (orderDateEpochMillis > 0L) orderDateEpochMillis else System.currentTimeMillis(),
            discountType = discountType.name,
            discountPercentage = discountPercentage.coerceIn(0.0, 100.0),
            discountAmount = discountAmount.coerceAtLeast(0.0),
            grossSales = grossSales.coerceAtLeast(0.0),
            netSales = netSales.coerceAtLeast(0.0),
            totalProductCost = totalProductCost.coerceAtLeast(0.0),
            estimatedProfit = estimatedProfit,
            paymentStatus = paymentStatus.name,
            amountPaid = amountPaid.coerceAtLeast(0.0),
            outstandingBalance = outstandingBalance.coerceAtLeast(0.0),
            paymentNotes = paymentNotes.trim(),
            remarks = remarks.trim(),
            lineItemsSummary = summary.trim()
        )
    }

    fun FirestoreTransaction.toRoomTransaction(customerId: Long? = null): SalesTransaction {
        val safeBusinessType = parseBusinessType(businessType)
        val safeDiscountType = parseDiscountType(discountType)
        val safeGrossSales = if (grossSales.isNaN() || grossSales < 0.0) 0.0 else grossSales
        val safeNetSales = if (netSales.isNaN() || netSales < 0.0) safeGrossSales else netSales
        val safeAmountPaid = if (amountPaid.isNaN() || amountPaid < 0.0) 0.0 else amountPaid
        val safeBalance = (safeNetSales - safeAmountPaid).coerceAtLeast(0.0)

        val resolvedStatus = parsePaymentStatus(paymentStatus, safeAmountPaid, safeNetSales)

        return SalesTransaction(
            id = id.trim().ifEmpty { UUID.randomUUID().toString() },
            orderNumber = orderNumber.trim().ifEmpty { "UNKNOWN-ORDER" },
            businessType = safeBusinessType,
            customerId = customerId,
            customerName = customerName.trim().ifEmpty { "Walk-in Customer" },
            orderDateEpochMillis = if (orderDateEpochMillis > 0L) orderDateEpochMillis else System.currentTimeMillis(),
            discountType = safeDiscountType,
            discountPercentage = discountPercentage.coerceIn(0.0, 100.0),
            discountAmount = discountAmount.coerceAtLeast(0.0),
            grossSales = safeGrossSales,
            netSales = safeNetSales,
            totalProductCost = if (totalProductCost.isNaN() || totalProductCost < 0.0) 0.0 else totalProductCost,
            estimatedProfit = if (estimatedProfit.isNaN()) 0.0 else estimatedProfit,
            paymentStatus = resolvedStatus,
            amountPaid = safeAmountPaid,
            outstandingBalance = safeBalance,
            paymentNotes = paymentNotes.trim(),
            remarks = remarks.trim(),
            createdAt = orderDateEpochMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    // --- Settings Mapping & Parsing ---

    fun AppSettings.toFirestore(userId: String): FirestoreSettings {
        return FirestoreSettings(
            id = "default",
            userId = userId,
            businessNameEngraving = businessNameEngraving.trim(),
            businessNameSintra = businessNameSintra.trim(),
            engravingOrderPrefix = engravingOrderPrefix.trim().ifEmpty { "ENG" },
            sintraOrderPrefix = sintraOrderPrefix.trim().ifEmpty { "SNT" },
            sintraDefaultMarginPercent = sintraDefaultMarginPercent.coerceIn(0.0, 100.0),
            sintraUseCostCalculation = sintraUseCostCalculation,
            currencySymbol = currencySymbol.trim().ifEmpty { "₱" }
        )
    }

    fun FirestoreSettings.toRoomSettings(id: Int = 1): AppSettings {
        val safeMargin = if (sintraDefaultMarginPercent.isNaN()) 58.3333 else sintraDefaultMarginPercent.coerceIn(0.0, 100.0)

        return AppSettings(
            id = id,
            businessNameEngraving = businessNameEngraving.trim().ifEmpty { "Engraving Business" },
            businessNameSintra = businessNameSintra.trim().ifEmpty { "Sintra Board Printing Business" },
            engravingOrderPrefix = engravingOrderPrefix.trim().ifEmpty { "ENG" },
            sintraOrderPrefix = sintraOrderPrefix.trim().ifEmpty { "SNT" },
            sintraDefaultMarginPercent = safeMargin,
            sintraUseCostCalculation = sintraUseCostCalculation,
            currencySymbol = currencySymbol.trim().ifEmpty { "₱" }
        )
    }

    // --- Resilient Enum Parsers with Fallbacks ---

    fun parseBusinessType(raw: String?): BusinessType {
        if (raw.isNullOrBlank()) return BusinessType.ENGRAVING
        return try {
            BusinessType.valueOf(raw.trim().uppercase())
        } catch (_: Exception) {
            when {
                raw.contains("sintra", ignoreCase = true) -> BusinessType.SINTRA_BOARD
                else -> BusinessType.ENGRAVING
            }
        }
    }

    fun parseDiscountType(raw: String?): DiscountType {
        if (raw.isNullOrBlank()) return DiscountType.PERCENTAGE
        return try {
            DiscountType.valueOf(raw.trim().uppercase())
        } catch (_: Exception) {
            when {
                raw.contains("fixed", ignoreCase = true) -> DiscountType.FIXED
                else -> DiscountType.PERCENTAGE
            }
        }
    }

    fun parsePaymentStatus(raw: String?, amountPaid: Double, netSales: Double): PaymentStatus {
        val parsed = try {
            if (!raw.isNullOrBlank()) PaymentStatus.valueOf(raw.trim().uppercase()) else null
        } catch (_: Exception) {
            null
        }

        // Preserve CANCELLED or REFUNDED if explicitly set
        if (parsed == PaymentStatus.CANCELLED) return PaymentStatus.CANCELLED

        return when {
            amountPaid >= netSales && netSales > 0.0 -> PaymentStatus.PAID
            amountPaid > 0.0 -> PaymentStatus.PARTIALLY_PAID
            parsed != null -> parsed
            else -> PaymentStatus.PENDING
        }
    }
}
