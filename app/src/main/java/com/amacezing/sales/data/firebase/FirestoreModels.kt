package com.amacezing.sales.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
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
    val isActive: Boolean = true,
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
    val isProfitMasked: Boolean = false,
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
    @ServerTimestamp val updatedAt: Timestamp? = null
)

