package br.com.viavalesistemas.manutencaobike.data.repositorio

import android.net.Uri
import br.com.viavalesistemas.manutencaobike.BuildConfig
import br.com.viavalesistemas.manutencaobike.data.DataError
import br.com.viavalesistemas.manutencaobike.data.comTratamentoDeErros
import br.com.viavalesistemas.manutencaobike.data.local.BikeDao
import br.com.viavalesistemas.manutencaobike.data.local.BikeEntity
import br.com.viavalesistemas.manutencaobike.data.strava.StravaApi
import br.com.viavalesistemas.manutencaobike.data.strava.StravaAuthApi
import br.com.viavalesistemas.manutencaobike.data.strava.StravaTokenStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StravaRepository @Inject constructor(
    private val api: StravaApi,
    private val authApi: StravaAuthApi,
    private val tokenStore: StravaTokenStore,
    private val bikeDao: BikeDao,
) {
    val configurado: Boolean =
        BuildConfig.STRAVA_CLIENT_ID.isNotBlank() && BuildConfig.STRAVA_CLIENT_SECRET.isNotBlank()

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
        val token = authApi.trocarCodigo(BuildConfig.STRAVA_CLIENT_ID, BuildConfig.STRAVA_CLIENT_SECRET, codigo)
        tokenStore.salvar(token)
    }

    suspend fun desconectar() {
        tokenStore.tokens()?.let { tokens ->
            // Falhar ao revogar no Strava não deve impedir a desconexão local.
            comTratamentoDeErros { authApi.desautorizar(tokens.accessToken) }
        }
        tokenStore.limpar()
    }

    /** Importa/atualiza as bikes do Strava e devolve quantas foram sincronizadas. */
    suspend fun sincronizarBikes(): Result<Int> = comTratamentoDeErros {
        val atleta = api.atleta()
        atleta.bikes.forEach { resumo ->
            val detalhe = api.equipamento(resumo.id)
            val km = resumo.distance / METROS_POR_KM
            val nome = detalhe.name ?: resumo.name ?: "Bike"
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
        atleta.bikes.size
    }.onFailure { if (it is DataError.NaoConectado) tokenStore.limpar() }

    companion object {
        const val ESQUEMA_CALLBACK = "manutencaobike"
        // O host precisa coincidir com o "Authorization Callback Domain" configurado no Strava.
        const val REDIRECT_URI = "$ESQUEMA_CALLBACK://localhost/strava"
        // profile:read_all é o que libera a lista de bikes do atleta.
        const val ESCOPOS = "read,profile:read_all"
        private const val METROS_POR_KM = 1000.0
    }
}
