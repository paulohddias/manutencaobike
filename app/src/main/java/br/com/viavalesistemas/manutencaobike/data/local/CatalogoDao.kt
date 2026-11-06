package br.com.viavalesistemas.manutencaobike.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogoDao {
    @Query("SELECT * FROM tipos_peca ORDER BY nome COLLATE NOCASE")
    fun observarTiposPeca(): Flow<List<TipoPecaEntity>>

    @Query("SELECT * FROM tipos_servico ORDER BY nome COLLATE NOCASE")
    fun observarTiposServico(): Flow<List<TipoServicoEntity>>

    @Query("SELECT * FROM tipos_servico")
    suspend fun listarTiposServico(): List<TipoServicoEntity>

    @Insert
    suspend fun inserirTipoPeca(tipo: TipoPecaEntity): Long

    @Update
    suspend fun atualizarTipoPeca(tipo: TipoPecaEntity)

    @Query("DELETE FROM tipos_peca WHERE id = :id")
    suspend fun excluirTipoPeca(id: Long)

    @Insert
    suspend fun inserirTipoServico(tipo: TipoServicoEntity): Long

    @Update
    suspend fun atualizarTipoServico(tipo: TipoServicoEntity)

    @Query("DELETE FROM tipos_servico WHERE id = :id")
    suspend fun excluirTipoServico(id: Long)
}
