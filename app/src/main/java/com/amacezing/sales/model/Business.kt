package com.amacezing.sales.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A business (entity/group) such as Engraving or Sintra Board Printing.
 * Orders, products and quotations reference a business by [Business.id]; that value is
 * stored in their existing `businessType` text column, so no existing rows change.
 */
typealias BusinessType = String

/** How estimated profit is derived for a business. */
enum class ProfitMethod(val displayName: String) {
    /** profit = net sales - total unit cost */
    UNIT_COST("Use unit costs"),

    /** profit = net sales x [Business.marginPercent] / 100 */
    FIXED_MARGIN("Fixed margin %")
}

@Entity(tableName = "businesses")
data class Business(
    @PrimaryKey
    val id: String,
    val name: String,
    val orderPrefix: String,
    val colorHex: String,
    val profitMethod: ProfitMethod,
    val marginPercent: Double,
    /** FIXED_MARGIN only: use real unit costs when the order has them (legacy Sintra toggle). */
    val useUnitCostIfAvailable: Boolean,
    val isActive: Boolean,
    val sortOrder: Int,
    /** Key into the app's icon set (see BusinessIcons); null means the default store icon. */
    val iconKey: String? = null
) {
    fun toProfitRule(): ProfitRule = ProfitRule(profitMethod, marginPercent, useUnitCostIfAvailable)

    companion object {
        const val ENGRAVING_ID = "ENGRAVING"
        const val SINTRA_BOARD_ID = "SINTRA_BOARD"

        /**
         * The two businesses the app originally shipped with. NOT seeded any more: they exist only so
         * data from older backups/installs that refer to them still maps to sensible names, colours and rules.
         */
        val DEFAULTS: List<Business> = listOf(
            Business(
                id = ENGRAVING_ID,
                name = "Engraving",
                orderPrefix = "ENG",
                colorHex = "#00B8F0",
                profitMethod = ProfitMethod.UNIT_COST,
                marginPercent = 0.0,
                useUnitCostIfAvailable = false,
                isActive = true,
                sortOrder = 0
            ),
            Business(
                id = SINTRA_BOARD_ID,
                name = "Sintra Board",
                orderPrefix = "SNT",
                colorHex = "#A78BFA",
                profitMethod = ProfitMethod.FIXED_MARGIN,
                marginPercent = 58.3333,
                useUnitCostIfAvailable = false,
                isActive = true,
                sortOrder = 1
            )
        )

        /** Readable name for an id whose Business row is missing (e.g. data restored from the cloud). */
        fun fallbackName(id: String): String =
            id.lowercase().split('_', ' ').filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                .ifBlank { "Business" }
    }
}

/** The pure inputs the profit calculation needs; keeps SalesCalculator free of database types. */
data class ProfitRule(
    val method: ProfitMethod = ProfitMethod.UNIT_COST,
    val marginPercent: Double = 0.0,
    val useUnitCostIfAvailable: Boolean = false
)
