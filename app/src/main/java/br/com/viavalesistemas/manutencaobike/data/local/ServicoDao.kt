package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

private const val SELECT_COM_TIPO =
    "SELECT s.*, t.nome AS tipoNome FROM servicos s INNER JOIN tipos_servico t ON t.id = s.tipoServicoId"

@Dao
interface ServicoDao {
    @Query("$SELECT_COM_TIPO WHERE s.bikeId = :bikeId ORDER BY s.data DESC, s.km DESC")
    fun observarDaBike(bikeId: Long): Flow<List<ServicoComTipo>>

    @Query(SELECT_COM_TIPO)
    fun observarTodos(): Flow<List<ServicoComTipo>>

    @Query(SELECT_COM_TIPO)
    suspend fun listarTodos(): List<ServicoComTipo>

    @Insert
    suspend fun inserir(servico: ServicoEntity): Long

    @Query("DELETE FROM servicos WHERE id = :id")
    suspend fun excluir(id: Long)
}
