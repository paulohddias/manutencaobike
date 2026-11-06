package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.viavalesistemas.manutencaobike.model.Servico
import java.time.LocalDate

@Entity(
    tableName = "servicos",
    foreignKeys = [
        ForeignKey(entity = BikeEntity::class, parentColumns = ["id"], childColumns = ["bikeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TipoServicoEntity::class, parentColumns = ["id"], childColumns = ["tipoServicoId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("bikeId"), Index("tipoServicoId")],
)
data class ServicoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bikeId: Long,
    val tipoServicoId: Long,
    val data: LocalDate,
    val km: Double,
    val custoCentavos: Long?,
    val observacoes: String?,
)

data class ServicoComTipo(
    @Embedded val servico: ServicoEntity,
    val tipoNome: String,
)

fun ServicoComTipo.toModel() = with(servico) {
    Servico(id, bikeId, tipoServicoId, tipoNome, data, km, custoCentavos, observacoes)
}

fun Servico.toEntity() = ServicoEntity(id, bikeId, tipoServicoId, data, km, custoCentavos, observacoes)
