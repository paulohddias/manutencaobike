package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

class Conversores {
    @TypeConverter
    fun deEpochDay(valor: Long?): LocalDate? = valor?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun paraEpochDay(data: LocalDate?): Long? = data?.toEpochDay()
}
