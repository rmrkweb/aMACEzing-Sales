package com.amacezing.sales.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Central business profile storing the business branding, contact information,
 * and document preferences. Reusable across future quotation generation, receipts,
 * invoices, and business reports.
 */
@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey
    val id: String = "active_profile",
    val userId: String = "",
    val businessName: String = "aMACEzing Printing Services",
    val tagline: String = "Precision Laser Engraving & Sintra Board Printing Solutions",
    val ownerName: String = "",
    val contactNumber: String = "",
    val contactEmail: String = "",
    val address: String = "",
    val facebookPage: String = "",
    val website: String = "",
    val tin: String = "",
    val documentFooter: String = "Thank you for your business! For inquiries and custom orders, contact us anytime.",
    val logoUri: String = "",
    val currency: String = "PHP",
    val quotationValidityDays: Int = 7,
    val paymentInstructions: String = "GCash: 0912-345-6789 / Bank Transfer: BDO 1234-5678-90 / Cash upon order confirmation",
    val quotationTerms: String = "1. Quotation is valid for 7 days from issuance.\n2. 50% non-refundable downpayment required before production.\n3. Turnaround time starts upon approval of final layout.",
    val receiptFooter: String = "This serves as an official sales acknowledgment. All items subject to quality inspection.",
    val showLogoOnDocuments: Boolean = true,
    val showContactOnDocuments: Boolean = true,
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
) {
    /**
     * Creates an immutable snapshot of this branding profile to be saved alongside
     * finalized quotations, receipts, or invoices, ensuring historical document accuracy.
     */
    fun toSnapshot(): BusinessProfileSnapshot {
        return BusinessProfileSnapshot(
            businessName = businessName,
            tagline = tagline,
            ownerName = ownerName,
            contactNumber = contactNumber,
            contactEmail = contactEmail,
            address = address,
            tin = tin,
            logoUri = logoUri,
            currency = currency,
            documentFooter = documentFooter,
            receiptFooter = receiptFooter,
            paymentInstructions = paymentInstructions,
            quotationTerms = quotationTerms,
            showLogoOnDocuments = showLogoOnDocuments,
            showContactOnDocuments = showContactOnDocuments,
            snapshotTimestamp = System.currentTimeMillis()
        )
    }
}

/**
 * Immutable snapshot stored with historical documents.
 */
data class BusinessProfileSnapshot(
    val businessName: String,
    val tagline: String,
    val ownerName: String,
    val contactNumber: String,
    val contactEmail: String,
    val address: String,
    val tin: String,
    val logoUri: String,
    val currency: String,
    val documentFooter: String,
    val receiptFooter: String,
    val paymentInstructions: String,
    val quotationTerms: String,
    val showLogoOnDocuments: Boolean,
    val showContactOnDocuments: Boolean,
    val snapshotTimestamp: Long = System.currentTimeMillis()
)
