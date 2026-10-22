package com.amacezing.sales.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUserProfile(
    val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreBusiness(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val type: String = "ENGRAVING",
    val orderPrefix: String = "ENG",
    // Added with dynamic businesses. Null on older documents, which then use the original defaults.
    val colorHex: String? = null,
    val profitMethod: String? = null,
    val marginPercent: Double? = null,
    val useUnitCostIfAvailable: Boolean? = null,
    val isActive: Boolean? = null,
    val sortOrder: Int? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreProduct(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val businessType: String = "ENGRAVING",
    val sellingPrice: Double = 0.0,
    val unitCost: Double = 0.0,
    // Kotlin "isXxx" properties are mapped to "xxx" by Firestore unless named explicitly.
    @get:PropertyName("isActive") @set:PropertyName("isActive") var isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreCustomer(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreLineItem(
    val productId: Long = 0L,
    val productName: String = "",
    val quantity: Int = 0,
    val unitSellingPrice: Double = 0.0,
    val unitCost: Double = 0.0,
    val lineSubtotal: Double = 0.0
)

@IgnoreExtraProperties
data class FirestorePayment(
    val amount: Double = 0.0,
    val paymentDateEpochMillis: Long = 0L,
    val paymentMethod: String = "Cash",
    val referenceNumber: String = "",
    val notes: String = ""
)

@IgnoreExtraProperties
data class FirestoreTransaction(
    val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val businessType: String = "ENGRAVING",
    val customerName: String = "",
    val orderDateEpochMillis: Long = 0L,
    val discountType: String = "PERCENTAGE",
    val discountPercentage: Double = 0.0,
    val discountAmount: Double = 0.0,
    val grossSales: Double = 0.0,
    val netSales: Double = 0.0,
    val totalProductCost: Double = 0.0,
    val estimatedProfit: Double = 0.0,
    val paymentStatus: String = "PENDING",
    val amountPaid: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val paymentNotes: String = "",
    val remarks: String = "",
    val lineItemsSummary: String = "",
    val lineItems: List<FirestoreLineItem> = emptyList(),
    val payments: List<FirestorePayment> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreSettings(
    val id: String = "default",
    val userId: String = "",
    val businessNameEngraving: String = "Engraving Business",
    val businessNameSintra: String = "Sintra Board Printing Business",
    val engravingOrderPrefix: String = "ENG",
    val sintraOrderPrefix: String = "SNT",
    val sintraDefaultMarginPercent: Double = 58.3333,
    val sintraUseCostCalculation: Boolean = false,
    val currencySymbol: String = "₱",
    val isProfitMasked: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreBusinessProfile(
    val id: String = "active_profile",
    val userId: String = "",
    val businessName: String = "aMACEzing Printing Services",
    val tagline: String = "",
    val ownerName: String = "",
    val contactNumber: String = "",
    val contactEmail: String = "",
    val address: String = "",
    val facebookPage: String = "",
    val website: String = "",
    val tin: String = "",
    val documentFooter: String = "",
    val logoUri: String = "",
    val currency: String = "PHP",
    val quotationValidityDays: Int = 7,
    val paymentInstructions: String = "",
    val quotationTerms: String = "",
    val receiptFooter: String = "",
    val showLogoOnDocuments: Boolean = true,
    val showContactOnDocuments: Boolean = true,
    /** Small PNG copy of the logo so it can be restored on another device / after logout. */
    val logoBase64: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

