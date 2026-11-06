package br.com.viavalesistemas.manutencaobike.data.repositorio

import android.net.Uri
import br.com.viavalesistemas.manutencaobike.data.DataError
import br.com.viavalesistemas.manutencaobike.data.comTratamentoDeErros
import br.com.viavalesistemas.manutencaobike.data.local.BikeDao
import br.com.viavalesistemas.manutencaobike.data.local.BikeEntity
import br.com.viavalesistemas.manutencaobike.data.local.FotosBike
import br.com.viavalesistemas.manutencaobike.data.local.toEntity
import br.com.viavalesistemas.manutencaobike.data.local.toModel
import br.com.viavalesistemas.manutencaobike.model.Bike
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BikeRepository @Inject constructor(
    private val dao: BikeDao,
    private val fotos: FotosBike,
) {

    /** Todas as bikes, ativas primeiro. */
    fun observarBikes(): Flow<List<Bike>> = dao.observarTodas().map { it.map(BikeEntity::toModel) }

    fun observarBike(id: Long): Flow<Bike?> = dao.observar(id).map { it?.toModel() }

    suspend fun listarAtivas(): List<Bike> = dao.listarAtivas().map(BikeEntity::toModel)

    fun arquivoDaFoto(bike: Bike): File? = bike.foto?.let(fotos::arquivo)

    fun arquivoDaFoto(nome: String): File = fotos.arquivo(nome)

    suspend fun salvar(bike: Bike): Result<Long> = comTratamentoDeErros {
        if (bike.id == 0L) {
            dao.inserir(bike.toEntity())
        } else {
            val fotoAnterior = dao.buscar(bike.id)?.foto
            dao.atualizar(bike.toEntity())
            if (fotoAnterior != null && fotoAnterior != bike.foto) descartarFoto(fotoAnterior)
            bike.id
        }
    }

    suspend fun atualizarKm(id: Long, km: Double): Result<Unit> = comTratamentoDeErros { dao.atualizarKm(id, km) }

    suspend fun definirAtiva(id: Long, ativa: Boolean): Result<Unit> =
        comTratamentoDeErros { dao.definirInativa(id, !ativa) }

    suspend fun excluir(id: Long): Result<Unit> = comTratamentoDeErros {
        val foto = dao.buscar(id)?.foto
        dao.excluir(id)
        foto?.let { descartarFoto(it) }
    }

    // Fora do comTratamentoDeErros: aqui um IOException é imagem ilegível, não falta de internet.
    suspend fun importarFoto(uri: Uri): Result<String> =
        try {
            Result.success(fotos.importar(uri))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(DataError.Foto(e))
        }

    suspend fun descartarFoto(nome: String) = withContext(Dispatchers.IO) { fotos.excluir(nome) }

    fun uriParaCamera(): Uri = fotos.uriParaCamera()

    fun limparCapturas() = fotos.limparCapturas()
}
