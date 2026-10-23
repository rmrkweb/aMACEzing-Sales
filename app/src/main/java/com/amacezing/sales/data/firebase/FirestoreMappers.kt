package com.amacezing.sales.data.firebase

import com.amacezing.sales.model.AppSettings
import com.amacezing.sales.model.BusinessProfile
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.Customer
import com.amacezing.sales.model.DiscountType
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.model.Product
import com.amacezing.sales.model.PaymentRecord
import com.amacezing.sales.model.SalesTransaction
import com.amacezing.sales.model.TransactionLineItem
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
            name = name.trim().ifEmpty { "Unnamed Product" },
            businessType = businessType,
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
            name = name.trim().ifEmpty { "Walk-in Customer" },
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

    fun SalesTransaction.toFirestore(
        userId: String,
        summary: String = "",
        lineItems: List<TransactionLineItem> = emptyList(),
        payments: List<PaymentRecord> = emptyList()
    ): FirestoreTransaction {
        return FirestoreTransaction(
            id = id.ifBlank { UUID.randomUUID().toString() },
            userId = userId,
            orderNumber = orderNumber.trim().ifEmpty { id.ifBlank { "UNKNOWN-ORDER" } },
            businessType = businessType,
            customerName = customerName.trim().ifEmpty { "Walk-in Customer" },
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
            lineItemsSummary = summary.trim(),
            lineItems = lineItems.map {
                FirestoreLineItem(
                    productId = it.productId,
                    productName = it.productName,
                    quantity = it.quantity,
                    unitSellingPrice = it.unitSellingPrice,
                    unitCost = it.unitCost,
                    lineSubtotal = it.lineSubtotal
                )
            },
            payments = payments.map {
                FirestorePayment(
                    amount = it.amount,
                    paymentDateEpochMillis = it.paymentDateEpochMillis,
                    paymentMethod = it.paymentMethod,
                    referenceNumber = it.referenceNumber,
                    notes = it.notes
                )
            }
        )
    }

    fun FirestoreTransaction.toRoomLineItems(): List<TransactionLineItem> =
        lineItems.filter { it.quantity > 0 }.map {
            TransactionLineItem(
                transactionId = id.trim(),
                productId = it.productId,
                productName = it.productName,
                quantity = it.quantity,
                unitSellingPrice = it.unitSellingPrice,
                unitCost = it.unitCost,
                lineSubtotal = it.lineSubtotal
            )
        }

    fun FirestoreTransaction.toRoomPayments(): List<PaymentRecord> =
        payments.filter { it.amount > 0.0 }.map {
            PaymentRecord(
                transactionId = id.trim(),
                amount = it.amount,
                paymentDateEpochMillis = if (it.paymentDateEpochMillis > 0L) it.paymentDateEpochMillis else System.currentTimeMillis(),
                paymentMethod = it.paymentMethod,
                referenceNumber = it.referenceNumber,
                notes = it.notes
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

    // --- Business Profile Mapping & Parsing ---

    fun BusinessProfile.toFirestore(userId: String): FirestoreBusinessProfile {
        return FirestoreBusinessProfile(
            id = id.ifBlank { "active_profile" },
            userId = userId,
            businessName = businessName.trim().ifEmpty { "aMACEzing Printing Services" },
            tagline = tagline.trim(),
            ownerName = ownerName.trim(),
            contactNumber = contactNumber.trim(),
            contactEmail = contactEmail.trim(),
            address = address.trim(),
            facebookPage = facebookPage.trim(),
            website = website.trim(),
            tin = tin.trim(),
            documentFooter = documentFooter.trim(),
            logoUri = logoUri.trim(),
            currency = currency.trim().ifEmpty { "PHP" },
            quotationValidityDays = if (quotationValidityDays > 0) quotationValidityDays else 7,
            paymentInstructions = paymentInstructions.trim(),
            quotationTerms = quotationTerms.trim(),
            receiptFooter = receiptFooter.trim(),
            showLogoOnDocuments = showLogoOnDocuments,
            showContactOnDocuments = showContactOnDocuments
        )
    }

    fun FirestoreBusinessProfile.toRoomBusinessProfile(id: String = "active_profile"): BusinessProfile {
        val safeValidity = if (quotationValidityDays > 0) quotationValidityDays else 7
        return BusinessProfile(
            id = id,
            userId = userId,
            businessName = businessName.trim().ifEmpty { "aMACEzing Printing Services" },
            tagline = tagline.trim(),
            ownerName = ownerName.trim(),
            contactNumber = contactNumber.trim(),
            contactEmail = contactEmail.trim(),
            address = address.trim(),
            facebookPage = facebookPage.trim(),
            website = website.trim(),
            tin = tin.trim(),
            documentFooter = documentFooter.trim().ifEmpty { "Thank you for your business! For inquiries, contact us anytime." },
            logoUri = logoUri.trim(),
            currency = currency.trim().ifEmpty { "PHP" },
            quotationValidityDays = safeValidity,
            paymentInstructions = paymentInstructions.trim(),
            quotationTerms = quotationTerms.trim(),
            receiptFooter = receiptFooter.trim().ifEmpty { "Official sales payment acknowledgment." },
            showLogoOnDocuments = showLogoOnDocuments,
            showContactOnDocuments = showContactOnDocuments,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
    }

    // --- Resilient Enum Parsers with Fallbacks ---

    /** Cloud document id is the lower-case business id, matching the documents seeded earlier. */
    fun com.amacezing.sales.model.Business.toFirestore(userId: String): FirestoreBusiness =
        FirestoreBusiness(
            id = id.lowercase(),
            userId = userId,
            name = name,
            type = id,
            orderPrefix = orderPrefix,
            colorHex = colorHex,
            profitMethod = profitMethod.name,
            marginPercent = marginPercent,
            useUnitCostIfAvailable = useUnitCostIfAvailable,
            isActive = isActive,
            sortOrder = sortOrder,
            iconKey = iconKey
        )

    /** Older documents lack the newer fields; they fall back to the original defaults for that business. */
    fun FirestoreBusiness.toRoomBusiness(fallbackSortOrder: Int): com.amacezing.sales.model.Business {
        val businessId = parseBusinessType(type)
        val default = com.amacezing.sales.model.Business.DEFAULTS.firstOrNull { it.id == businessId }
        return com.amacezing.sales.model.Business(
            id = businessId,
            name = name.trim().ifEmpty { default?.name ?: com.amacezing.sales.model.Business.fallbackName(businessId) },
            orderPrefix = orderPrefix.trim().uppercase().ifEmpty { default?.orderPrefix ?: "ORD" },
            colorHex = colorHex ?: default?.colorHex ?: "#9AA6B2",
            profitMethod = profitMethod?.let {
                try { com.amacezing.sales.model.ProfitMethod.valueOf(it) } catch (_: Exception) { null }
            } ?: default?.profitMethod ?: com.amacezing.sales.model.ProfitMethod.UNIT_COST,
            marginPercent = (marginPercent ?: default?.marginPercent ?: 0.0).let {
                if (it.isNaN() || it < 0.0) 0.0 else it.coerceAtMost(100.0)
            },
            useUnitCostIfAvailable = useUnitCostIfAvailable ?: default?.useUnitCostIfAvailable ?: false,
            isActive = isActive ?: true,
            sortOrder = sortOrder ?: default?.sortOrder ?: fallbackSortOrder,
            iconKey = iconKey
        )
    }

    /** Business ids are free-form now; normalise legacy spellings and keep unknown ids intact. */
    fun parseBusinessType(raw: String?): BusinessType {
        if (raw.isNullOrBlank()) return com.amacezing.sales.model.Business.ENGRAVING_ID
        val normalized = raw.trim().uppercase().replace(' ', '_')
        return when {
            normalized == com.amacezing.sales.model.Business.ENGRAVING_ID ||
                normalized == com.amacezing.sales.model.Business.SINTRA_BOARD_ID -> normalized
            raw.contains("sintra", ignoreCase = true) -> com.amacezing.sales.model.Business.SINTRA_BOARD_ID
            else -> normalized
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
