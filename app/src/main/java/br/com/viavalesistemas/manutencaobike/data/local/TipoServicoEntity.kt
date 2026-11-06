package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.viavalesistemas.manutencaobike.model.TipoServico

@Entity(tableName = "tipos_servico")
data class TipoServicoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val intervaloKm: Int?,
    val intervaloMeses: Int?,
)

fun TipoServicoEntity.toModel() = TipoServico(id, nome, intervaloKm, intervaloMeses)

fun TipoServico.toEntity() = TipoServicoEntity(id, nome, intervaloKm, intervaloMeses)
