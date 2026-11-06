package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.viavalesistemas.manutencaobike.model.Bike

@Entity(tableName = "bikes", indices = [Index(value = ["stravaGearId"], unique = true)])
data class BikeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val marca: String?,
    val modelo: String?,
    val kmAtual: Double,
    val stravaGearId: String?,
    val arquivada: Boolean = false,
)

fun BikeEntity.toModel() = Bike(id, nome, marca, modelo, kmAtual, stravaGearId, arquivada)

fun Bike.toEntity() = BikeEntity(id, nome, marca, modelo, kmAtual, stravaGearId, arquivada)
