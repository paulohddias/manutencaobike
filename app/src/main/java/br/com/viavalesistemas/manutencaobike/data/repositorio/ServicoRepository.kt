package br.com.viavalesistemas.manutencaobike.data.repositorio

import br.com.viavalesistemas.manutencaobike.data.comTratamentoDeErros
import br.com.viavalesistemas.manutencaobike.data.local.ServicoComTipo
import br.com.viavalesistemas.manutencaobike.data.local.ServicoDao
import br.com.viavalesistemas.manutencaobike.data.local.toEntity
import br.com.viavalesistemas.manutencaobike.data.local.toModel
import br.com.viavalesistemas.manutencaobike.model.Servico
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServicoRepository @Inject constructor(private val dao: ServicoDao) {

    fun observarDaBike(bikeId: Long): Flow<List<Servico>> =
        dao.observarDaBike(bikeId).map { it.map(ServicoComTipo::toModel) }

    fun observarTodos(): Flow<List<Servico>> = dao.observarTodos().map { it.map(ServicoComTipo::toModel) }

    suspend fun listarTodos(): List<Servico> = dao.listarTodos().map(ServicoComTipo::toModel)

    suspend fun registrar(servico: Servico): Result<Long> = comTratamentoDeErros { dao.inserir(servico.toEntity()) }

    suspend fun excluir(servicoId: Long): Result<Unit> = comTratamentoDeErros { dao.excluir(servicoId) }
}
