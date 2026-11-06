package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.viavalesistemas.manutencaobike.model.TipoPeca

@Entity(tableName = "tipos_peca")
data class TipoPecaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val vidaUtilKmPadrao: Int?,
)

fun TipoPecaEntity.toModel() = TipoPeca(id, nome, vidaUtilKmPadrao)

fun TipoPeca.toEntity() = TipoPecaEntity(id, nome, vidaUtilKmPadrao)
