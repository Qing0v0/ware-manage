package com.example.myapplication.utils

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.util.Date

class DataConvertor {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun stringToBig(decimalText: String): BigDecimal {
        return BigDecimal(decimalText)
    }

    @TypeConverter
    fun bigToString(bigDecimal: BigDecimal): String {
        // 用文本存价格（比如 "40.5"），既不丢小数也不会像 Double 那样有精度问题。
        // 老版本这里存的是 Int，小数会被截掉，所以两个数据库都升到了版本 2
        return bigDecimal.toPlainString()
    }
}