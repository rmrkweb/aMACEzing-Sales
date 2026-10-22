package com.amacezing.sales.data

import androidx.room.TypeConverter
import com.amacezing.sales.model.DiscountType
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.model.ProfitMethod

class Converters {
    @TypeConverter
    fun fromProfitMethod(value: ProfitMethod): String = value.name

    @TypeConverter
    fun toProfitMethod(value: String): ProfitMethod = try {
        ProfitMethod.valueOf(value)
    } catch (_: Exception) {
        ProfitMethod.UNIT_COST
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

    @TypeConverter
    fun fromQuotationStatus(value: com.amacezing.sales.model.QuotationStatus): String = value.name

    @TypeConverter
    fun toQuotationStatus(value: String): com.amacezing.sales.model.QuotationStatus = try {
        com.amacezing.sales.model.QuotationStatus.valueOf(value)
    } catch (_: Exception) {
        com.amacezing.sales.model.QuotationStatus.DRAFT
    }
}
