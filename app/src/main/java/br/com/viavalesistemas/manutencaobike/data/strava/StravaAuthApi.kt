package br.com.viavalesistemas.manutencaobike.data.strava

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/** Só o que dispensa o client_secret; troca e renovação de token passam pelo nosso servidor. */
interface StravaAuthApi {
    @FormUrlEncoded
    @POST("oauth/deauthorize")
    suspend fun desautorizar(@Field("access_token") accessToken: String)
}
