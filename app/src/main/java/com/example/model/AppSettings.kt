package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val businessNameEngraving: String = "Engraving Business",
    val businessNameSintra: String = "Sintra Board Printing Business",
    val engravingOrderPrefix: String = "ENG",
    val sintraOrderPrefix: String = "SNT",
    val sintraDefaultMarginPercent: Double = 58.3333, // Default 58.3333% from existing Excel workbook
    val sintraUseCostCalculation: Boolean = false,    // Toggle between margin and actual unit costs
    val currencySymbol: String = "₱",
    val defaultDiscountPercentage: Double = 0.0,
    val nextEngravingSequence: Int = 1,
    val nextSintraSequence: Int = 1
)
