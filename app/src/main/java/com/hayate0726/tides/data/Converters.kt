package com.hayate0726.tides.data

import androidx.room.TypeConverter
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun localDateToEpochDay(d: LocalDate?): Long? = d?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epoch: Long?): LocalDate? = epoch?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun flowToInt(f: FlowIntensity): Int = f.intCode

    @TypeConverter
    fun flowFromInt(i: Int): FlowIntensity = FlowIntensity.fromInt(i)

    @TypeConverter
    fun symptomToString(s: Symptom): String = s.name

    @TypeConverter
    fun symptomFromString(s: String): Symptom = Symptom.valueOf(s)

    @TypeConverter
    fun bcMethodToString(m: BirthControlMethod): String = m.name

    @TypeConverter
    fun bcMethodFromString(s: String): BirthControlMethod = BirthControlMethod.valueOf(s)

    @TypeConverter
    fun goalToString(g: com.hayate0726.tides.domain.model.Goal): String = g.name

    @TypeConverter
    fun goalFromString(s: String): com.hayate0726.tides.domain.model.Goal =
        com.hayate0726.tides.domain.model.Goal.valueOf(s)
}
