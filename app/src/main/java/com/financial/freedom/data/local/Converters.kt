package com.financial.freedom.data.local

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

class Converters {
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun bigDecimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }
}
