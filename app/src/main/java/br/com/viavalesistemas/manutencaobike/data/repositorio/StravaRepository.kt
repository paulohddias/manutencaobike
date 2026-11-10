package br.com.viavalesistemas.manutencaobike.data.repositorio

import android.net.Uri
import br.com.viavalesistemas.manutencaobike.BuildConfig
import br.com.viavalesistemas.manutencaobike.data.DataError
import br.com.viavalesistemas.manutencaobike.data.comTratamentoDeErros
import br.com.viavalesistemas.manutencaobike.data.local.BikeDao
import br.com.viavalesistemas.manutencaobike.data.local.BikeEntity
import br.com.viavalesistemas.manutencaobike.data.servidor.CodigoDto
import br.com.viavalesistemas.manutencaobike.data.servidor.VvsApi
import br.com.viavalesistemas.manutencaobike.data.strava.StravaApi
import br.com.viavalesistemas.manutencaobike.data.strava.StravaAuthApi
import br.com.viavalesistemas.manutencaobike.data.strava.StravaTokenStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class BikeExcluida(val gearId: String, val nome: String)

/** [excluidas]: bikes que ainda existem no Strava, mas o usuário excluiu no app. */
data class ResultadoSincronizacao(val importadas: Int, val excluidas: List<BikeExcluida>)

@Singleton
class StravaRepository @Inject constructor(
    private val api: StravaApi,
    private val authApi: StravaAuthApi,
    private val vvsApi: VvsApi,
    private val tokenStore: StravaTokenStore,
    private val bikeDao: BikeDao,
) {
    val configurado: Boolean = BuildConfig.STRAVA_CLIENT_ID.isNotBlank()

    val conectado: Flow<Boolean> = tokenStore.conectado
    val nomeAtleta: Flow<String?> = tokenStore.nomeAtleta

    fun urlAutorizacao(): String = Uri.parse("https://www.strava.com/oauth/mobile/authorize").buildUpon()
        .appendQueryParameter("client_id", BuildConfig.STRAVA_CLIENT_ID)
        .appendQueryParameter("redirect_uri", REDIRECT_URI)
        .appendQueryParameter("response_type", "code")
        .appendQueryParameter("approval_prompt", "auto")
        .appendQueryParameter("scope", ESCOPOS)
        .build()
        .toString()

    suspend fun conectar(codigo: String): Result<Unit> = comTratamentoDeErros {
        tokenStore.salvarConexao(vvsApi.conectarStrava(CodigoDto(codigo)))
    }.recoverCatching { erro ->
        throw if (erro is DataError.Servidor && erro.codigo == 403) DataError.LimiteStrava(erro) else erro
    }

    suspend fun desconectar() {
        tokenStore.tokens()?.let { tokens ->
            // Falhar ao revogar no Strava ou no servidor não deve impedir a desconexão local.
            comTratamentoDeErros { authApi.desautorizar(tokens.accessToken) }
        }
        comTratamentoDeErros { vvsApi.encerrarSessao() }
        tokenStore.limpar()
    }

    /** Importa/atualiza as bikes do Strava, pulando as que o usuário excluiu no app. */
    suspend fun sincronizarBikes(): Result<ResultadoSincronizacao> = comTratamentoDeErros {
        val atleta = api.atleta()
        val ignorados = bikeDao.gearsIgnorados().toSet()
        val (excluidas, bikes) = atleta.bikes.partition { it.id in ignorados }
        bikes.forEach { resumo ->
            val detalhe = api.equipamento(resumo.id)
            val km = resumo.distance / METROS_POR_KM
            val nome = (detalhe.name ?: resumo.name)?.trim()?.ifEmpty { null } ?: "Bike"
            val aposentada = resumo.retired || detalhe.retired
            val existente = bikeDao.buscarPorStrava(resumo.id)
            if (existente == null) {
                bikeDao.inserir(
                    BikeEntity(
                        nome = nome,
                        marca = detalhe.brandName,
                        modelo = detalhe.modelName,
                        kmAtual = km,
                        stravaGearId = resumo.id,
                        inativa = aposentada,
                    ),
                )
            } else {
                // Ativa/inativa e foto são escolhas do usuário no app; a sincronização não mexe nelas.
                bikeDao.atualizar(
                    existente.copy(
                        nome = nome,
                        marca = detalhe.brandName ?: existente.marca,
                        modelo = detalhe.modelName ?: existente.modelo,
                        kmAtual = km,
                    ),
                )
            }
        }
        ResultadoSincronizacao(
            importadas = bikes.size,
            excluidas = excluidas.map { BikeExcluida(it.id, it.name?.trim()?.ifEmpty { null } ?: "Bike") },
        )
    }.onFailure { if (it is DataError.NaoConectado) tokenStore.limpar() }

    /** Tira as bikes da lista de excluídas e importa de novo, agora sem o histórico antigo. */
    suspend fun restaurarExcluidas(gearIds: List<String>): Result<Int> {
        comTratamentoDeErros { bikeDao.restaurarIgnoradas(gearIds) }.onFailure { return Result.failure(it) }
        return sincronizarBikes().map { gearIds.size }
    }

    companion object {
        const val ESQUEMA_CALLBACK = "manutencaobike"
        // O host precisa coincidir com o "Authorization Callback Domain" configurado no Strava.
        const val REDIRECT_URI = "$ESQUEMA_CALLBACK://localhost/strava"
        // profile:read_all é o que libera a lista de bikes do atleta.
        const val ESCOPOS = "read,profile:read_all"
        private const val METROS_POR_KM = 1000.0
    }
}
