package br.com.viavalesistemas.manutencaobike.data.repositorio

import br.com.viavalesistemas.manutencaobike.data.comTratamentoDeErros
import br.com.viavalesistemas.manutencaobike.data.local.PecaComTipo
import br.com.viavalesistemas.manutencaobike.data.local.PecaDao
import br.com.viavalesistemas.manutencaobike.data.local.toEntity
import br.com.viavalesistemas.manutencaobike.data.local.toModel
import br.com.viavalesistemas.manutencaobike.model.Peca
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PecaRepository @Inject constructor(private val dao: PecaDao) {

    fun observarDaBike(bikeId: Long): Flow<List<Peca>> = dao.observarDaBike(bikeId).map { it.map(PecaComTipo::toModel) }

    fun observarInstaladas(): Flow<List<Peca>> = dao.observarInstaladas().map { it.map(PecaComTipo::toModel) }

    suspend fun listarInstaladas(): List<Peca> = dao.listarInstaladas().map(PecaComTipo::toModel)

    suspend fun instalar(peca: Peca): Result<Long> = comTratamentoDeErros { dao.inserir(peca.toEntity()) }

    suspend fun remover(pecaId: Long, kmBike: Double, data: LocalDate): Result<Unit> =
        comTratamentoDeErros { dao.marcarRemovida(pecaId, kmBike, data) }

    suspend fun excluir(pecaId: Long): Result<Unit> = comTratamentoDeErros { dao.excluir(pecaId) }
}
