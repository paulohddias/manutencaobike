package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

private const val SELECT_COM_TIPO =
    "SELECT p.*, t.nome AS tipoNome FROM pecas p INNER JOIN tipos_peca t ON t.id = p.tipoPecaId"

@Dao
interface PecaDao {
    @Query("$SELECT_COM_TIPO WHERE p.bikeId = :bikeId ORDER BY p.dataInstalacao DESC, p.id DESC")
    fun observarDaBike(bikeId: Long): Flow<List<PecaComTipo>>

    @Query("$SELECT_COM_TIPO WHERE p.kmRemocao IS NULL")
    fun observarInstaladas(): Flow<List<PecaComTipo>>

    @Query("$SELECT_COM_TIPO WHERE p.kmRemocao IS NULL")
    suspend fun listarInstaladas(): List<PecaComTipo>

    @Insert
    suspend fun inserir(peca: PecaEntity): Long

    @Query("UPDATE pecas SET kmRemocao = :km, dataRemocao = :data WHERE id = :id")
    suspend fun marcarRemovida(id: Long, km: Double, data: LocalDate)

    @Query("DELETE FROM pecas WHERE id = :id")
    suspend fun excluir(id: Long)
}
