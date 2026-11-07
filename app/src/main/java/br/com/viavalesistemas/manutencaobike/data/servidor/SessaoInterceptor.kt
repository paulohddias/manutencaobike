package br.com.viavalesistemas.manutencaobike.data.servidor

import br.com.viavalesistemas.manutencaobike.data.strava.StravaTokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Envia o token de sessão do nosso servidor quando existe; strava/token é a única rota pública. */
class SessaoInterceptor @Inject constructor(
    private val tokenStore: StravaTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val sessao = runBlocking { tokenStore.sessao() } ?: return chain.proceed(chain.request())
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $sessao")
            .build()
        return chain.proceed(request)
    }
}
