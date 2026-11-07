package br.com.viavalesistemas.manutencaobike.di

import br.com.viavalesistemas.manutencaobike.BuildConfig
import br.com.viavalesistemas.manutencaobike.data.servidor.SessaoInterceptor
import br.com.viavalesistemas.manutencaobike.data.servidor.VvsApi
import br.com.viavalesistemas.manutencaobike.data.strava.StravaApi
import br.com.viavalesistemas.manutencaobike.data.strava.StravaAuthApi
import br.com.viavalesistemas.manutencaobike.data.strava.StravaAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StravaAuthClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val URL_STRAVA = "https://www.strava.com/"
    private const val URL_API_STRAVA = "https://www.strava.com/api/v3/"

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun logging(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        // BASIC e não BODY: o corpo das respostas de OAuth contém os tokens.
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private fun clienteBase(logging: HttpLoggingInterceptor) = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)

    // Cliente separado, sem o interceptor de autenticação: é ele que renova o token.
    @Provides
    @Singleton
    @StravaAuthClient
    fun clienteAuth(logging: HttpLoggingInterceptor): OkHttpClient = clienteBase(logging).build()

    @Provides
    @Singleton
    fun clienteApi(logging: HttpLoggingInterceptor, auth: StravaAuthInterceptor): OkHttpClient =
        clienteBase(logging).addInterceptor(auth).build()

    // Sem o interceptor do Strava: é por aqui que o token do Strava é renovado.
    // Timeout maior porque também transporta os backups.
    @Provides
    @Singleton
    fun vvsApi(logging: HttpLoggingInterceptor, sessao: SessaoInterceptor, json: Json): VvsApi {
        val cliente = clienteBase(logging)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .addInterceptor(sessao)
            .build()
        return retrofit(BuildConfig.API_URL, cliente, json).create()
    }

    @Provides
    @Singleton
    fun stravaAuthApi(@StravaAuthClient client: OkHttpClient, json: Json): StravaAuthApi =
        retrofit(URL_STRAVA, client, json).create()

    @Provides
    @Singleton
    fun stravaApi(client: OkHttpClient, json: Json): StravaApi = retrofit(URL_API_STRAVA, client, json).create()

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
