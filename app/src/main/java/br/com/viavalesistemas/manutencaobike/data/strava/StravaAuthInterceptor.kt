package br.com.viavalesistemas.manutencaobike.data.strava

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class StravaAuthInterceptor @Inject constructor(
    private val sessao: StravaSessao,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // runBlocking é seguro aqui: roda numa thread do OkHttp, e a renovação usa outro OkHttpClient.
        val token = try {
            runBlocking { sessao.tokenValido() }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            // Só IOException é propagada pelo OkHttp; o resto derrubaria o app.
            throw IOException("Falha ao renovar o token do Strava", e)
        }
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
