package br.com.viavalesistemas.manutencaobike.data.repositorio

import br.com.viavalesistemas.manutencaobike.data.comTratamentoDeErros
import br.com.viavalesistemas.manutencaobike.data.local.CatalogoDao
import br.com.viavalesistemas.manutencaobike.data.local.TipoPecaEntity
import br.com.viavalesistemas.manutencaobike.data.local.TipoServicoEntity
import br.com.viavalesistemas.manutencaobike.data.local.toEntity
import br.com.viavalesistemas.manutencaobike.data.local.toModel
import br.com.viavalesistemas.manutencaobike.model.TipoPeca
import br.com.viavalesistemas.manutencaobike.model.TipoServico
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogoRepository @Inject constructor(private val dao: CatalogoDao) {

    fun observarTiposPeca(): Flow<List<TipoPeca>> = dao.observarTiposPeca().map { it.map(TipoPecaEntity::toModel) }

    fun observarTiposServico(): Flow<List<TipoServico>> =
        dao.observarTiposServico().map { it.map(TipoServicoEntity::toModel) }

    suspend fun listarTiposServico(): List<TipoServico> = dao.listarTiposServico().map(TipoServicoEntity::toModel)

    suspend fun salvarTipoPeca(tipo: TipoPeca): Result<Unit> = comTratamentoDeErros {
        if (tipo.id == 0L) dao.inserirTipoPeca(tipo.toEntity()) else dao.atualizarTipoPeca(tipo.toEntity())
    }

    suspend fun excluirTipoPeca(id: Long): Result<Unit> = comTratamentoDeErros { dao.excluirTipoPeca(id) }

    suspend fun salvarTipoServico(tipo: TipoServico): Result<Unit> = comTratamentoDeErros {
        if (tipo.id == 0L) dao.inserirTipoServico(tipo.toEntity()) else dao.atualizarTipoServico(tipo.toEntity())
    }

    suspend fun excluirTipoServico(id: Long): Result<Unit> = comTratamentoDeErros { dao.excluirTipoServico(id) }
}
