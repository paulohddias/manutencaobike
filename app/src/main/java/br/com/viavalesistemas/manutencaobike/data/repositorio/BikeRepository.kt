package br.com.viavalesistemas.manutencaobike.data.repositorio

import br.com.viavalesistemas.manutencaobike.data.comTratamentoDeErros
import br.com.viavalesistemas.manutencaobike.data.local.BikeDao
import br.com.viavalesistemas.manutencaobike.data.local.BikeEntity
import br.com.viavalesistemas.manutencaobike.data.local.toEntity
import br.com.viavalesistemas.manutencaobike.data.local.toModel
import br.com.viavalesistemas.manutencaobike.model.Bike
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BikeRepository @Inject constructor(private val dao: BikeDao) {

    fun observarBikes(): Flow<List<Bike>> = dao.observarAtivas().map { it.map(BikeEntity::toModel) }

    fun observarBike(id: Long): Flow<Bike?> = dao.observar(id).map { it?.toModel() }

    suspend fun listarBikes(): List<Bike> = dao.listarAtivas().map(BikeEntity::toModel)

    suspend fun salvar(bike: Bike): Result<Long> = comTratamentoDeErros {
        if (bike.id == 0L) {
            dao.inserir(bike.toEntity())
        } else {
            dao.atualizar(bike.toEntity())
            bike.id
        }
    }

    suspend fun atualizarKm(id: Long, km: Double): Result<Unit> = comTratamentoDeErros { dao.atualizarKm(id, km) }

    suspend fun excluir(id: Long): Result<Unit> = comTratamentoDeErros { dao.excluir(id) }
}
