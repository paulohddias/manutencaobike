package br.com.viavalesistemas.manutencaobike.data.servidor

import br.com.viavalesistemas.manutencaobike.data.strava.TokenDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

/** API própria (servidor PHP em servidor/), que guarda o client_secret do Strava e os backups. */
interface VvsApi {
    @POST("strava/token")
    suspend fun conectarStrava(@Body corpo: CodigoDto): ConexaoDto

    @POST("strava/refresh")
    suspend fun renovarStrava(@Body corpo: RefreshDto): TokenDto

    @DELETE("sessao")
    suspend fun encerrarSessao()

    @GET("backups")
    suspend fun listarBackups(): ListaBackupsDto

    @POST("backups")
    suspend fun enviarBackup(@Body zip: RequestBody): BackupDto

    @Streaming
    @GET("backups/{id}")
    suspend fun baixarBackup(@Path("id") id: String): ResponseBody

    @DELETE("conta")
    suspend fun excluirConta()
}

@Serializable
data class CodigoDto(val code: String)

@Serializable
data class RefreshDto(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class ConexaoDto(val sessao: String, val token: TokenDto)

@Serializable
data class ListaBackupsDto(val backups: List<BackupDto>)

@Serializable
data class BackupDto(
    val id: String,
    @SerialName("criado_em") val criadoEm: Long,
    val tamanho: Long,
)
