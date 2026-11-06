package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.viavalesistemas.manutencaobike.model.Peca
import java.time.LocalDate

@Entity(
    tableName = "pecas",
    foreignKeys = [
        ForeignKey(entity = BikeEntity::class, parentColumns = ["id"], childColumns = ["bikeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TipoPecaEntity::class, parentColumns = ["id"], childColumns = ["tipoPecaId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("bikeId"), Index("tipoPecaId")],
)
data class PecaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bikeId: Long,
    val tipoPecaId: Long,
    val descricao: String?,
    val kmInstalacao: Double,
    val vidaUtilKm: Int,
    val dataInstalacao: LocalDate,
    val custoCentavos: Long?,
    val kmRemocao: Double?,
    val dataRemocao: LocalDate?,
)

data class PecaComTipo(
    @Embedded val peca: PecaEntity,
    val tipoNome: String,
)

fun PecaComTipo.toModel() = with(peca) {
    Peca(id, bikeId, tipoPecaId, tipoNome, descricao, kmInstalacao, vidaUtilKm, dataInstalacao, custoCentavos, kmRemocao, dataRemocao)
}

fun Peca.toEntity() =
    PecaEntity(id, bikeId, tipoPecaId, descricao, kmInstalacao, vidaUtilKm, dataInstalacao, custoCentavos, kmRemocao, dataRemocao)
