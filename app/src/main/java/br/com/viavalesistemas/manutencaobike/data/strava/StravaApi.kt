package br.com.viavalesistemas.manutencaobike.data.strava

import retrofit2.http.GET
import retrofit2.http.Path

interface StravaApi {
    @GET("athlete")
    suspend fun atleta(): AtletaDto

    @GET("gear/{id}")
    suspend fun equipamento(@Path("id") id: String): EquipamentoDto
}
