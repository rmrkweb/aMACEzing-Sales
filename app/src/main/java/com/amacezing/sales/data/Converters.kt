package com.amacezing.sales.data

import androidx.room.TypeConverter
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.DiscountType
import com.amacezing.sales.model.PaymentStatus

class Converters {
    @TypeConverter
    fun fromBusinessType(value: BusinessType): String = value.name

    @TypeConverter
    fun toBusinessType(value: String): BusinessType = try {
        BusinessType.valueOf(value)
    } catch (_: Exception) {
        BusinessType.ENGRAVING
    }

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String = value.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus = try {
        PaymentStatus.valueOf(value)
    } catch (_: Exception) {
        PaymentStatus.PENDING
    }

    @TypeConverter
    fun fromDiscountType(value: DiscountType): String = value.name

    @TypeConverter
    fun toDiscountType(value: String): DiscountType = try {
        DiscountType.valueOf(value)
    } catch (_: Exception) {
        DiscountType.PERCENTAGE
    }
}
