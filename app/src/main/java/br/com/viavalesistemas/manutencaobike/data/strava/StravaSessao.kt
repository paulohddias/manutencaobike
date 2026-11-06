package br.com.viavalesistemas.manutencaobike.data.strava

import br.com.viavalesistemas.manutencaobike.BuildConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Entrega um access token válido, renovando-o quando expira (o do Strava dura 6 h). */
@Singleton
class StravaSessao @Inject constructor(
    private val authApi: StravaAuthApi,
    private val tokenStore: StravaTokenStore,
) {
    // Evita que várias requisições simultâneas renovem o token ao mesmo tempo.
    private val mutex = Mutex()

    suspend fun tokenValido(): String = mutex.withLock {
        val tokens = tokenStore.tokens() ?: throw StravaNaoConectadoException()
        if (tokens.expiraEm - MARGEM_SEGUNDOS > Instant.now().epochSecond) return tokens.accessToken

        val novo = try {
            authApi.renovar(BuildConfig.STRAVA_CLIENT_ID, BuildConfig.STRAVA_CLIENT_SECRET, tokens.refreshToken)
        } catch (e: HttpException) {
            if (e.code() == 400 || e.code() == 401) {
                tokenStore.limpar()
                throw StravaNaoConectadoException(e)
            }
            throw e
        }
        tokenStore.salvar(novo)
        novo.accessToken
    }

    private companion object {
        const val MARGEM_SEGUNDOS = 120
    }
}
